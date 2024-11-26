### Задачи центрального сервера
- Обработка запросов от клиента
    - Запрос на создание тонеля RequestTube(to) [Создаем тонели используя MakeTube, используя рандомный id]
    - Запрос на закрытие тонеля RequestCloseTube(id) [Закрываем тонель на всех пользователях связанных с ним]
    - Запрос на авторизацию Auth(name, password) [Авторизируем пользователя и возвращаем ему новый пароль. Запоминаем пользователя]
    - Запрос на отключение пользователя Exit() [Исключаем пользователя из множества узлов для туннелирования]

## Примиры работы "на пальцах"

### Общая последовательность установки/разрыва соединения
```
[Client]: Auth(name, password)
[Server]: NextPassword(password)
...
[Client]: Exit()
[Server]: Rebuilding all related tunnels (locally)
[Server]: Fetching tunnels reinitialization
```

### Последовательность создания тунеля
- Обозначения
  - node0 - сервер узла отправляющего запрос на создание туннеля (по умолчанию)
  - nodek (k in [0, n)) - сервер узла туннеля, через который данные проходят насквозь
  - noden - сервер узла, принимающего соединение

```
[FrontS->Client]: RequestTube(endpoint_user_id, local_id)
[Client->Server]: RequestTube(endpoint_user_id, local_id)
[Server->Client] (noden): RequestConnection(request_user)
[Client->FrontS] (noden): RequestConnection(request_user)
[FrontS->Client] (noden): ConnectionResponse(is_allowed, reason)
[Client->Server] (noden): ConnectionResponse(is_allowed, reason)
[Server->Client]: ConnectionResponse(is_allowed, reason, local_id)
[Client->FrontS]: ConnectionResponse(is_allowed, reason, local_id)
In case of allowed connection:
[Server]: Building tunnel structure
[Server->Client]: MakeTube(null, node1, tunnel_id)
[Server->Client] (node1): MakeTube(node0, node2, tunnel_id)
[Server->Client] (node2): MakeTube(node1, node3, tunnel_id)
Same for each node of tunnel.
[Server->Client] (noden): MakeTube(noden-1, null, tunnel_id)
[Server->Client]: EstablishConnection(tunnel_id, local_id, endpoint_user)
[Client->FrontS]: EstablishConnection(local_id, local_port)
[Server->Client] (noden): EstablishConnection(tunnel_id, local_id, begpoint_user)
[Client->FrontS] (noden): EstablishConnection(local_id, local_port)
```