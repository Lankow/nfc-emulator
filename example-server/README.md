# Example External Server

This Node.js server demonstrates how an external controller can interact with
the NFC Emulator app using the same JSON API exposed by the internal server.
It keeps AIDs, scenarios and a communication log in memory and can handle
multiple requests concurrently.

## Setup

```bash
cd example-server
npm install
```

## Running

```bash
npm start
```

The server listens on port `1818` by default. Use the `PORT` environment
variable to change it.

## Endpoints

All commands are sent as `POST` requests with a JSON body to the root path `/`.
The payload structure matches the [HTTP Control API](../README.md#http-control-api)
of the app. Example:

```bash
curl -X POST http://localhost:1818/ -H "Content-Type: application/json" \
  -d '{"Type":"Aid","Add":"A0000002471001"}'
```
