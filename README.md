configurando o cluster:
```
nano /storage/rabbitmq2/.erlang.cookie
nano /storage/rabbitmq3/.erlang.cookie
nano /storage/rabbitmq4/.erlang.cookie
#colocar o mesmo texto chave no arquivo .erlang.cookie
```
subir o cluster rabbitmq:
```
docker-compose up
```
tornar o cluster intergrado:
```
docker exec -it rabbitmq3 bash
rabbitmqctl cluster_status
rabbitmqctl stop_app
rabbitmqctl join_cluster rabbit@rabbitmq2
rabbitmqctl start_app
docker exec -it rabbitmq4 bash
rabbitmqctl stop_app
rabbitmqctl join_cluster rabbit@rabbitmq2
rabbitmqctl start_app
```


subir o rabbitmq:
```
docker run -it --rm --name rabbitmq -p 5552:5552 -p 5672:5672 -p 15672:15672 -p 15692:15692  -e RABBITMQ_SERVER_ADDITIONAL_ERL_ARGS='-rabbitmq_stream advertised_host localhost' rabbitmq:3-management
```


aplicar policies:  
entrar no shell do rabbitmq:  
```
docker exec -it rabbitmq bash
```
aplicar as policies:  
```
rabbitmqctl set_policy AE "^my-exch-direct$" '{"alternate-exchange":"my-ae"}' --apply-to exchanges

rabbitmqctl set_policy my-pol "^minhaqueue$" \
  '{"max-length-bytes":1048,"overflow":"reject-publish"}' \
  --apply-to queues
```
