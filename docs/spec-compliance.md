# Matrix specification compliance

What a Matrix identity server must provide, according to the
[Identity Service API](https://spec.matrix.org/latest/identity-service-api/)
(checked against spec v1.19), and what ma1sd serves.

## Identity Service API

All paths are below `/_matrix/identity`. "Auth" is the identity server access
token that the client gets from `account/register`. The spec requires the server
to accept it both in the `Authorization: Bearer` header and in the deprecated
`access_token` query parameter; ma1sd accepts both (header first).

| Method | Path | Auth | ma1sd |
|---|---|---|---|
| GET | `/versions` | no | yes, `{"versions": ["r0.3.0", "v1.1"]}` |
| GET | `/v2` (status) | no | yes |
| GET | `/v2/account` | yes | yes |
| POST | `/v2/account/register` | no (OpenID token) | yes |
| POST | `/v2/account/logout` | yes | yes (GET is accepted too) |
| GET | `/v2/terms` | no | yes |
| POST | `/v2/terms` | yes | yes |
| GET | `/v2/pubkey/{keyId}` | no | yes |
| GET | `/v2/pubkey/isvalid` | no | yes |
| GET | `/v2/pubkey/ephemeral/isvalid` | no | yes |
| GET | `/v2/hash_details` | yes | yes, if `hashing.enabled` |
| POST | `/v2/lookup` | yes | yes, if `hashing.enabled` |
| POST | `/v2/validate/{email,msisdn}/requestToken` | yes | yes |
| GET, POST | `/v2/validate/{email,msisdn}/submitToken` | yes | yes |
| POST | `/v2/3pid/bind` | yes | yes |
| GET | `/v2/3pid/getValidated3pid` | yes | yes |
| POST | `/v2/3pid/unbind` | yes | yes |
| POST | `/v2/store-invite` | yes | yes |
| POST | `/v2/sign-ed25519` | yes | yes |

The `/v2` endpoints are served when `matrix.v2` is `true` (the default).

### Removed from the spec

Spec v1.1 removed the v1 identity API (`/_matrix/identity/api/v1/...`, including
`lookup` and `bulk_lookup`, MSC2713). ma1sd still serves it while `matrix.v1` is
`true`, for old clients and homeservers; set `matrix.v1: false` to turn it off.

Spec v1.11 deprecated the `access_token` query parameter in favour of the
`Authorization` header; servers must still accept it.

## Client-server API endpoints handled by ma1sd

ma1sd also answers some homeserver endpoints, to which the reverse proxy sends
the requests instead of to the homeserver. Spec v1.1 renamed the client-server
API version `r0` to `v3` (MSC2844) and current clients only use `v3`; ma1sd serves
both, so the reverse proxy only has to route them, without rewriting the path.

| Method | Path | Feature |
|---|---|---|
| GET, POST | `/_matrix/client/{r0,v3}/login` | [Authentication](features/authentication.md) |
| POST | `/_matrix/client/{r0,v3}/user_directory/search` | [Directory search](features/directory.md) |
| GET | `/_matrix/client/{r0,v3}/profile/{userId}` | [Profile](features/experimental/profile.md) |
| POST | `/_matrix/client/{r0,v3}/register/{medium}/requestToken` | [Registration](features/registration.md) |
| POST | `/_matrix/client/{r0,v3}/rooms/{roomId}/invite` | [Identity](features/identity.md) (3PID invite policy) |

nginx example routing all of them to ma1sd (before the general `/_matrix` entry):
```nginx
location ~ ^/_matrix/client/(r0|v3)/(login|user_directory/search|profile/[^/]+|register/[^/]+/requestToken|rooms/[^/]+/invite)$ {
    proxy_pass http://localhost:8090;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}
```

Requests ma1sd sends to the homeserver use the `v3` client-server API.

## Application service API

ma1sd can act as an application service (`/_matrix/app/v1/users/{userId}`,
`/_matrix/app/v1/transactions/{txnId}`, plus the legacy paths without prefix).
The homeserver token is taken from the `Authorization` header (spec v1.4, sent by
Synapse) or from the legacy `access_token` query parameter.
