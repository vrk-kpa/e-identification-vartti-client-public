# e-identification-vartti-client
e-identification-vartti-client

### Proxy -> Vartti-client
```
/person/{identifier}/{certSerial}?issuerCN={issuerCN}
```
Example request:
```
http://localhost:8080/vartti/person/00098705718/924600003018?issuerCN=VRK%20TEST%20CA%20for%20Healthcare%20Professionals
```

### Vartti-client -> Proxy

Example response:
```
{
  "success":true,
  "error":null,
  "varttiPerson":
  {
    "hetu":"010101-0101"
  }
}
```
