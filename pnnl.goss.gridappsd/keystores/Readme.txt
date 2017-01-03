Creation of keystores and truststores.

# Create an RSA key and adds/creates it in mybroker.ks  
# This is the private key for the alias broker.
#
# It requires a bit of information and a password for getting
# into the keystore.  We use the password GossServerTemp.
keytool -genkey -alias broker -keyalg RSA -keystore mybroker.ks

Enter keystore password: GossServerTemp
Re-enter new password:
What is your first and last name?
  [Unknown]:  GOSS TEST
What is the name of your organizational unit?
  [Unknown]:  USE AT OWN RISK
What is the name of your organization?
  [Unknown]:  NOT LIABLE
What is the name of your City or Locality?
  [Unknown]:  Richland
What is the name of your State or Province?
  [Unknown]:  WA
What is the two-letter country code for this unit?
  [Unknown]:  US
Is CN=GOSS TEST, OU=USE AT OWN RISK, O=NOT LIABLE, L=Richland, ST=WA, C=US correct?
  [no]:  yes
  
Enter key password for <broker>
        (RETURN if same as keystore password): JUST PRESS ENTER

# Generate a public key (certificate) for the alias broker and stores it
# into a file called mybroker_cert.  
keytool -export -alias broker -keystore mybroker.ks -file mybroker_cert

Enter keystore password: GossServerTemp
Certificate stored in file <mybroker_cert>

# Do the same thing that you did for the broker above but this time use 
# client as the alias.  We used the password GossClientTemp
keytool -genkey -alias client -keyalg RSA -keystore myclient.ks

Enter keystore password: GossClientTemp
Re-enter new password:
What is your first and last name?
  [Unknown]:  CLIENT TEST
What is the name of your organizational unit?
  [Unknown]:  USE AT OWN RISK
What is the name of your organization?
  [Unknown]:  NOT LIABLE
What is the name of your City or Locality?
  [Unknown]:  Richland
What is the name of your State or Province?
  [Unknown]:  WA
What is the two-letter country code for this unit?
  [Unknown]:  US
Is CN=CLIENT TEST, OU=USE AT OWN RISK, O=NOT LIABLE, L=Richland, ST=WA, C=US correct?
  [no]:  yes

Enter key password for <client>
        (RETURN if same as keystore password): JUST PRESS ENTER
		
# Next the creation of a trust store for the client to "trust" the 
# connection to the server will include the broker's certificate.
# We use the password GossClientTrust
keytool -import -alias broker -keystore myclient.ts -file mybroker_cert

Enter keystore password: GossClientTrust
Re-enter new password: GossClientTrust
Owner: CN=GOSS TEST, OU=USE AT OWN RISK, O=NOT LIABLE, L=Richland, ST=WA, C=US
Issuer: CN=GOSS TEST, OU=USE AT OWN RISK, O=NOT LIABLE, L=Richland, ST=WA, C=US
Serial number: 63285474
Valid from: Wed Mar 11 20:15:11 PDT 2015 until: Tue Jun 09 20:15:11 PDT 2015
Certificate fingerprints:
         MD5:  57:E9:D5:9F:7B:ED:E4:C0:42:1A:FC:FF:98:A6:B2:41
         SHA1: 86:06:58:2D:72:78:AC:BA:92:E9:97:C6:89:CD:F6:07:43:17:36:A8
         SHA256: F9:89:96:00:13:0E:6D:98:08:43:C9:3A:9A:D4:53:58:24:CC:9E:FB:0F:
53:87:71:5F:CC:B4:52:81:51:70:08
         Signature algorithm name: SHA256withRSA
         Version: 3

Extensions:

#1: ObjectId: 2.5.29.14 Criticality=false
SubjectKeyIdentifier [
KeyIdentifier [
0000: BA 12 EF 9D 12 67 3E 8C   B0 40 C8 37 CC 87 B5 F4  .....g>..@.7....
0010: E5 C4 BF E3                                        ....
]
]

Trust this certificate? [no]:  yes
Certificate was added to keystore

# If necessary we can check the client certification based upon a server trust store
# Step 1 Export the client cert from the client.ks
	keytool -export -alias client -keystore myclient.ks -file myclient_cert
	Enter keystore password: GossClientTemp
	Certificate stored in file <myclient_cert>

# Step 2 Create a trust store for the server to use 
	keytool -import -alias client -keystore myserver.ts -file myclient_cert
	Enter keystore password: GossServerTrust
	Re-enter new password:
	Owner: CN=CLIENT TEST, OU=USE AT OWN RISK, O=NOT LIABLE, L=Richland, ST=WA, C=US
	Issuer: CN=CLIENT TEST, OU=USE AT OWN RISK, O=NOT LIABLE, L=Richland, ST=WA, C=US
	Serial number: 5def6848
	Valid from: Wed Mar 11 20:18:13 PDT 2015 until: Tue Jun 09 20:18:13 PDT 2015
	Certificate fingerprints:
	         MD5:  E1:8D:91:18:2D:41:64:57:18:D1:A2:1E:99:20:A3:58
	         SHA1: 2A:B1:64:8B:5B:AA:FD:AB:5A:B7:7A:70:B0:6F:B8:8A:D8:B5:29:2B
	         SHA256: AD:34:08:35:7F:79:A5:EE:71:4D:89:94:C9:9C:3C:8B:E7:0B:01:B1:EE:F1:F9:E2:48:62:81:91:47:04:AD:1E
	         Signature algorithm name: SHA256withRSA
	         Version: 3
	
	Extensions:
	
	#1: ObjectId: 2.5.29.14 Criticality=false
	SubjectKeyIdentifier [
	KeyIdentifier [
	0000: FF F5 07 1F 17 D2 9B 1F   44 90 F0 75 DD E3 8C 2E  ........D..u....
	0010: 3E 2F AE 80                                        >/..
	]
	]
	
	Trust this certificate? [no]:  yes
	Certificate was added to keystore

