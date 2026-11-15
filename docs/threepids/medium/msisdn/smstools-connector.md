# SMS notifications - smstools connector
Connector ID: `smstools`

Sends SMS through [SMS Server Tools 3](http://smstools3.kekekasvi.com/) with a GSM modem,
via [smstools-http-api](https://github.com/lvasiliev/smstools-http-api). The API can run
on another host than ma1sd (e.g. the machine the modem is attached to).

ma1sd calls `POST {url}/api/v1.0/sms/outgoing` with `{"mobiles": ["<number>"], "text": "..."}`
and checks that the API answers `"response": "Ok"` for the number. Numbers are sent in
international format without `+` (e.g. `436761234567`).

## Configuration
```yaml
threepid:
  medium:
    msisdn:
      connector: 'smstools'
      connectors:
        smstools:
          url: 'http://sms.example.org:5000'   # base URL of smstools-http-api
          username: 'ma1sd'                     # HTTP basic auth user from the API config
          password: 'secret'
          queue: ''                             # optional smsd queue (default: API/smsd default)
          timeout: 30                           # seconds
```

If the API uses `USER_WHITELIST`, allow the numbers ma1sd sends to for that user.
