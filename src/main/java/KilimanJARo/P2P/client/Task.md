### Задачи сервера клиента
- Обработка запросов от сервера 
  - Запрос на прокладку туннеля MakeTube(from, to, tunnel_id) [В случае null аргументов, второй конец прокладывается на локальный порт]
    - Ответ содержит статус успешности выполнения
  - Запрос на закрытие туннеля CloseTube(tunnel_id)
    - NO RESPONSE
  - Запрос на установку соединения RequestConnection(request_user, tunnel_id) [Перенаправляется на фронт]
    - Ответ (is_allowed, reason, tunnel_id)
  - Запрос на финализацию установки соединения EstablishConnection(tunnel_id, local_id, endpoint_user) [Перенаправляется на фронт, обогащаясь портом, на который развернут туннель]
    - NO RESPONSE

- Обработка запросов от локального пользователя
  - Запрос на создание тонеля RequestTube(endpoint_name, local_id) [Перенаправляется на сервер]
    - Ответ перенаправляется с сервера
  - Запрос на закрытие тонеля RequestCloseTube(local_id) [Перенаправляется на сервер обогащаясь tunnel_id]
