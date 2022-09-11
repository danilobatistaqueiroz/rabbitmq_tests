cat /etc/hosts
ping rabbitmq2
ping rabbitmq3
sudo rabbitmqctl stop_app
exit
rabbitmqctl cluster_status
exit
sudo rabbitmqctl stop_app
rabbitmqctl stop_app
rabbitmqctl join_cluster rabbit@rabbitmq2
rabbitmqctl start_app
rabbitmqctl cluster_status
exit
