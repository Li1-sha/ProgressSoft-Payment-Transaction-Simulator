# curl -v Output

$ curl -v http://localhost:8080/orders

*   Trying [::1]:8080...
* Established connection to localhost (::1 port 8080) from ::1 port 49936
* using HTTP/1.x
> GET /orders HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/8.18.0
> Accept: */*
>
* Request completely sent off
< HTTP/1.1 200 OK
< Date: Thu, 13 Aug 2026 08:19:38 GMT
< Content-Type: text/plain;charset=iso-8859-1
< Content-Length: 193
< Server: Jetty(9.4.53.v20231009)
<
Order{id=1, customerName='Shahad', amount=67.00, currency='OMR'}
Order{id=2, customerName='Shahad', amount=67.00, currency='OMR'}
Order{id=3, customerName='Sara', amount=48.99, currency='OMR'}
* Connection #0 to host localhost left intact

## Observation

I was surprised that the `Server` header was added automatically by Jetty – I never set it in my servlet or filter. Also, the `Content-Length` was computed for me, even though I only set the `Content-Type` explicitly.