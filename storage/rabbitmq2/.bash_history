rabbitmqadmin -u guest -p guest declare exchange name=test_exchange type=direct
rabbitmqctl list_queues
rabbitmqadmin -u guest -p guest declare binding source="test_exchange" destination_type="queue" destination="qquorum1" routing_key="test_routing_key_quorum"
rabbitmqadmin -u guest -p guest get queue=test_quorum
rabbitmqadmin -u guest -p guest get queue=qquorum1
rabbitmqadmin -u guest -p guest publish exchange=test_exchange routing_key=test_routing_key_quorum payload="hello world, Quorum Queue"
rabbitmqctl list_queues
rabbitmqadmin -u guest -p guest get queue=qquorum1
rabbitmqctl list_queues
exit
poweroff
shutdown
exit
rabbitmqctl
rabbitmqctl set_policy DLX ".*" '{"dead-letter-exchange":"minha-dlx-queue"}' --apply-to queues
clear_policy DLX
rabbitmqctl clear_policy DLX
rabbitmqctl set_policy DLX ".*" '{"dead-letter-exchange":"minha-dlx-queue"}' --apply-to queues
rabbitmqctl clear_policy DLX
rabbitmqctl set_policy DLX ".*" '{"dead-letter-exchange":"minha-dlx-queue"}' --apply-to queues
rabbitmqctl set_policy DLX ".*" '{"dead-letter-exchange":"minha-dlx-queue"}' --apply-to queues
rabbitmqctl set_policy DLX '.*' '{"dead-letter-exchange":"minha-dlx-queue"}' --apply-to queues
rabbitmqctl clear_policy DLX
rabbitmqctl set_policy DLX '.*' '{'dead-letter-exchange':'minha-dlx-queue'}' --apply-to queues
rabbitmqctl set_policy DLX '.*' '{dead-letter-exchange:minha-dlx-queue}' --apply-to queues
rabbitmqctl set_policy DLX '.*' '{"dead-letter-exchange:minha-dlx-queue}' --apply-to queues
rabbitmqctl set_policy DLX '.*' '{"dead-letter-exchange":"minha-dlx-queue"}' --apply-to queues
rabbitmqctl clear_policy DLX
exit
