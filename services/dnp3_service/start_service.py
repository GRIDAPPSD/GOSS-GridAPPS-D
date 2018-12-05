import argparse
import logging
import numbers
import sys
from time import sleep

from yaml import safe_load

from pydnp3 import opendnp3
from dnp3.points import (
    PointArray, PointDefinitions, PointDefinition, DNP3Exception, POINT_TYPE_ANALOG_INPUT, POINT_TYPE_BINARY_INPUT
)
from dnp3.outstation import DNP3Outstation

logging.basicConfig(stream=sys.stdout, level=logging.DEBUG,
                    format='%(asctime)s:%(name)s:%(levelname)s: %(message)s')

_log = logging.getLogger(__name__)


class Processor(object):

    def __init__(self, point_definitions):
        self.point_definitions = point_definitions
        self._current_point_values = {}
        self._selector_block_points = {}
        self._current_array = None

    def publish_outstation_status(self, status):
        _log.debug(status)

    def process_point_value(self, command_type, command, index, op_type):
        """
            A point value was received from the Master. Process its payload.

        @param command_type: Either 'Select' or 'Operate'.
        @param command: A ControlRelayOutputBlock or else a wrapped data value (AnalogOutputInt16, etc.).
        @param index: DNP3 index of the payload's data definition.
        @param op_type: An OperateType, or None if command_type == 'Select'.
        @return: A CommandStatus value.
        """
        try:
            _log.debug("cmdtype={},command={},index={},optype={}".format(command_type, command, index, op_type))
            _log.debug("command_status={},command_value={}".format(command.status, command.value))
            point_value = self.point_definitions.point_value_for_command(command_type, command, index, op_type)
            if point_value is None:
                return opendnp3.CommandStatus.DOWNSTREAM_FAIL
        except Exception as ex:
            _log.error('No DNP3 PointDefinition for command with index {}'.format(index))
            return opendnp3.CommandStatus.DOWNSTREAM_FAIL

        try:
            self._process_point_value(point_value)
        except Exception as ex:
            _log.error('Error processing DNP3 command: {}'.format(ex))
            self.discard_cached_point_value(point_value)
            return opendnp3.CommandStatus.DOWNSTREAM_FAIL

        return opendnp3.CommandStatus.SUCCESS

    def add_to_current_values(self, value):
        """Update a dictionary that holds the most-recently-received value of each point."""
        self._current_point_values.setdefault(value.point_def.point_type, {})[int(value.index)] = value

    def get_point_named(self, point_name):
        return self.point_definitions.get_point_named(point_name)

    def for_point_type_and_index(self, point_type, index):
        return self.point_definitions.for_point_type_and_index(point_type, index)

    def discard_cached_point_value(self, point_value):
        """Delete a cached point value (typically occurs only if an error is being handled)."""
        try:
            self._current_point_values.get(point_value.point_def.point_type, {}).pop(int(point_value.index), None)
        except Exception as err:
            _log.error('Error discarding cached value {}'.format(point_value))

    def start_selector_block(self, point_value):
        """
            Fetch PointValues from self._selector_block_points for the point_value's Block and Edit Selector.
            Transfer the fetched PointValues to self._current_point_values.
            If an index in the block has no PointValue, null out that index in self._current_point_values, too.

        :param point_value: A PointValue that is the start of a selector block.
        """
        _log.debug('Starting to receive a selector block: {}'.format(point_value.name))
        point_def = point_value.point_def
        block_points = self._get_selector_block_points(point_value.name, point_value.unwrapped_value())
        pt_dict = {pt.index: pt for pt in block_points}
        for ind in range(point_def.selector_block_start, point_def.selector_block_end):
            if ind == point_def.index:
                pass                # Don't overwrite the selector block's main point (i.e., its edit selector)
            elif ind in pt_dict:
                self.add_to_current_values(pt_dict[ind])
            else:
                cached_points = self._current_point_values.setdefault(point_def.point_type, {})
                if ind in cached_points:
                    del cached_points[ind]

    def _get_selector_block_points(self, point_name, edit_selector):
        """Return cached selector block points for point_name and edit_selector."""
        return self._selector_block_points.get(point_name, {}).get(edit_selector, [])

    def save_selector_block(self, point_value):
        """
            Save a copy of the selector block that is referenced by point_value's save_on_write property.
        """
        block_name = point_value.point_def.save_on_write
        selector_block_point = self.get_point_named(block_name)
        edit_selector = self.get_current_point_value_for_def(selector_block_point).unwrapped_value()
        pt_vals_for_range = [self.get_current_point_value(selector_block_point.point_type, ind)
                             for ind in range(selector_block_point.selector_block_start,
                                              selector_block_point.selector_block_end)]
        if block_name not in self._selector_block_points:
            self._selector_block_points[block_name] = {}
        block_points = [val for val in pt_vals_for_range if val is not None]
        _log.debug('Saving {} points for {} at edit selector {}'.format(len(block_points),
                                                                        block_name,
                                                                        edit_selector))
        self._selector_block_points[block_name][edit_selector] = block_points

    def update_array_for_point(self, point_value):
        """A received point belongs to a PointArray. Update it."""
        if point_value.point_def.is_array_head_point:
            self._current_array = PointArray(point_value.point_def)
        elif self._current_array is None:
            raise DNP3Exception('Array point received, but there is no current Array.')
        elif not self._current_array.contains_index(point_value.index):
            raise DNP3Exception('Received Array point outside of current Array.')
        self._current_array.add_point_value(point_value)

    def update_input_point(self, point_def, value):
        """
            Update an input point. This may send its PointValue to the Master.

        :param point_def: A PointDefinition.
        :param value: A value to send (unwrapped simple data type, or else a list/array).
        """
        if type(value) == list:
            # It's an array. Break it down into its constituent points, and apply each one separately.
            col_count = len(point_def.array_points)
            cols_by_name = {pt['name']: col for col, pt in enumerate(point_def.array_points)}
            for row_number, point_dict in enumerate(value):
                for pt_name, pt_val in point_dict.iteritems():
                    pt_index = point_def.index + col_count * row_number + cols_by_name[pt_name]
                    array_point_def = self.point_definitions.get_point_named(point_def.name, index=pt_index)
                    self._apply_point_update(array_point_def, pt_index, pt_val)
        else:
            self._apply_point_update(point_def, point_def.index, value)

    @staticmethod
    def _apply_point_update(point_def, point_index, value):
        """
            Set an input point in the outstation database. This may send its PointValue to the Master.

        :param point_def: A PointDefinition.
        :param point_index: A numeric index for the point.
        :param value: A value to send (unwrapped, simple data type).
        """
        point_type = PointDefinition.point_type_for_group(point_def.group)
        if point_type == POINT_TYPE_ANALOG_INPUT:
            wrapped_val = opendnp3.Analog(float(value))
            if isinstance(value, bool) or not isinstance(value, numbers.Number):
                # Invalid data type
                raise DNP3Exception('Received {} value for {}.'.format(type(value), point_def))
        elif point_type == POINT_TYPE_BINARY_INPUT:
            wrapped_val = opendnp3.Binary(value)
            if not isinstance(value, bool):
                # Invalid data type
                raise DNP3Exception('Received {} value for {}.'.format(type(value), point_def))
        else:
            # The agent supports only DNP3's Analog and Binary point types at this time.
            raise DNP3Exception('Unsupported point type {}'.format(point_type))
        if wrapped_val is not None:
            DNP3Outstation.apply_update(wrapped_val, point_index)
        _log.debug('Sent DNP3 point {}, value={}'.format(point_def, wrapped_val.value))

    def _process_point_value(self, point_value):
        _log.info('Received DNP3 {}'.format(point_value))
        if point_value.command_type == 'Select':
            # Perform any needed validation now, then wait for the subsequent Operate command.
            return None
        else:
            self.add_to_current_values(point_value)
            if point_value.point_def.is_selector_block:
                self.start_selector_block(point_value)
            if point_value.point_def.save_on_write:
                self.save_selector_block(point_value)
            return point_value


def start_outstation(outstation_config, processor):

    dnp3_outstation = DNP3Outstation('0.0.0.0', 20000, outstation_config)
    dnp3_outstation.set_agent(processor)
    dnp3_outstation.start()
    _log.debug('DNP3 initialization complete. In command loop.')
    # Ad-hoc tests can be performed at this point if desired.
    return dnp3_outstation


def load_point_definitions(self):
    """
        Load and cache a dictionary of PointDefinitions from a json list.

        Index the dictionary by point_type and point index.
    """
    _log.debug('Loading DNP3 point definitions.')
    try:
        if type(self.points) == str:
            # There's something odd here. The point and function definitions are defined in the
            # config file using a 'config://' entry (previously used only by MasterDriveAgent).
            # It seems like this should have been resolved to the registry entry at which the
            # 'config://' entry points, and in that case 'self.points' should already be
            # a json structure. But instead, it's still the string 'config://mesa_points.config'.
            # The call to get_from_config_store() below works around the issue by fetching the linked
            # registry entry.
            point_defs = self.get_from_config_store(self.points)
        else:
            point_defs = self.points
        self.point_definitions = PointDefinitions()
        self.point_definitions.load_points(point_defs)
    except (AttributeError, TypeError) as err:
        if self._local_point_definitions_path:
            _log.warning("Attempting to load point definitions from local path.")
            self.point_definitions = PointDefinitions(point_definitions_path=self._local_point_definitions_path)
        else:
            raise DNP3Exception("Failed to load point definitions from config store: {}".format(err))


def publish_outstation_status(status_string):
    print(status_string)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()

    parser.add_argument('-c', '--config_file', required=True,
                        type=argparse.FileType('r'),
                        help="Yaml points and outstation configuration file.")
    args = parser.parse_args()

    full_dict = safe_load(args.config_file)
    points = full_dict.get('points')
    if not points:
        sys.stderr.write("invalid points specified in yaml configuration file.")
        sys.exit(10)

    oustation = full_dict.get('outstation', {})
    point_def = PointDefinitions()
    point_def.load_points(points)
    processor = Processor(point_def)
    #point_def.load_points(points)

    outstation = start_outstation(oustation, processor)

    try:
        while True:
            sleep(0.01)
    finally:
        outstation.shutdown()

