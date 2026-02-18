package gov.pnnl.goss.gridappsd.data;

public class BGResult {
    String subject;
    String property;
    String object;

    public BGResult(String s, String p, String o) {
        this.subject = s;
        this.property = p;
        this.object = o;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

}
