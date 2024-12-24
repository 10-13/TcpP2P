### Описание используемых структур
- Пользователь:
  - name (Имя пользователя)
  - pass (Пароль. Генерируется рандомно и меняется после каждой авторизации.)
- Туннель:
  - id (Выдается сервером в момент получения запроса на создание туннеля)
  - local_id (Выдается фронтом при попытке создания туннеля)
  - begpoint, endpoint (Пользователи заведующие туннелем (открывший и принявший соответственно). Являются теми кто общается и управляет туннелем.)
  - nodes (Пользователи через которых проходит туннель)
  - Взаимдействия:
    - Создать (по имени пользователя адресата)
    - Удалить (по local_id расширяется до local_id + id на клиенте)

### Задачи центрального сервера
- Обработка запросов от клиента:
  - Запрос на создание туннеля RequestTube(endpoint, local_id) [Создаем тонели используя MakeTube, используя рандомный id]
    - В качестве ответа летит сгенерироваанные метаданные туннеля (local_id, tunnel_id)
  - Ответ на создание туннеля ConnectionResponse(is_allowed, reason, tunnel_id) [Указывает был ли запрос принят]
    - !!! Возможно будет трансформирован в ответ на запрос
  - Запрос на закрытие тонеля RequestCloseTube(local_id, tunnel_id) [Закрываем тонель на всех пользователях связанных с ним. Может быть вызван только авторизированным пользователем]
    - NO RESPONSE
  - Запрос на регистрацию register() [Регистрируем пользователя и возвращаем ему пароль]
    - Ответ (is_success, message, password)
  - Запрос на авторизацию login(name, password) [Авторизируем пользователя и возвращаем ему новый пароль. Запоминаем пользователя для дальнейшего прокидывания туннелей]
    - Ответ (is_success, message, next_password)
  - Запрос на отключение пользователя logout() [Исключаем пользователя из множества узлов для туннелирования]
    - Ответ (is_success, message)


## Примиры работы "на пальцах"

### Общая последовательность установки/разрыва соединения
```
[Client->Server]: login(name, password) -> (next_password)
...
[Client->Server]: logout()
[Server]: Rebuilding all related tunnels (locally)
[Server]: Fetching tunnels reinitialization
```

### Последовательность создания тунеля
- Обозначения
  - node0 - сервер узла отправляющего запрос на создание туннеля (по умолчанию)
  - nodek (k in [0, n)) - сервер узла туннеля, через который данные проходят насквозь
  - noden - сервер узла, принимающего соединение

```
[FrontS->Client]: RequestTube(endpoint_name, local_id) -> (is_allowed, reason, local_id)
  [Client->Server]: RequestTube(endpoint_name, local_id) -> (is_allowed, reason, local_id, tunnel_id)
    [Server->Client] (noden): RequestConnection(request_user, tunnel_id) -> (is_allowed, reason, tunnel_id)
       [Client->FrontS] (noden): RequestConnection(request_user, tunnel_id) -> (is_allowed, reason, tunnel_id)
In case of allowed connection:
[Server]: Building tunnel structure
[Server->Client]: MakeTube(null, node1, tunnel_id) -> (is_completed, port)
[Server->Client] (node1): MakeTube(node0, node2, tunnel_id) -> (is_completed, port)
[Server->Client] (node2): MakeTube(node1, node3, tunnel_id) -> (is_completed, port)
Same for each node of tunnel.
[Server->Client] (noden): MakeTube(noden-1, null, tunnel_id) -> (is_completed, null)
[Server->Client]: EstablishConnection(tunnel_id, local_id, endpoint_user)
[Client->FrontS]: EstablishConnection(local_id, local_port)
[Server->Client] (noden): EstablishConnection(tunnel_id, local_id, begpoint_user)
[Client->FrontS] (noden): EstablishConnection(local_id, local_port)
```

- Важно обратить внимание на то, что вызовы MakeTube имеют строгий порядок, чтобы гарантировать существование открытого порта на предыдущем узле.