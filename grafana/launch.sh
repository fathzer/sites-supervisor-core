#An example of how to launch grafana with Docker
#TODO Anonymous connection is disabled ... don't know why :-(
ID=$(id -u)
#docker run -d --user $ID -v "/home/jma/grafana/data:/var/lib/grafana" -e "GF_SERVER_ROOT_URL=http://dev-sicg.labtech.pls.renault.fr:3000" -e "GF_SECURITY_ADMIN_PASSWORD=mastersicg" -p 3000:3000 --name grafana --restart always --dns 172.26.80.129 grafana/grafana:6.4.3
docker run -d --user $ID -v "/home/jma/grafana/data:/var/lib/grafana" -e "GF_AUTH_ANONYMOUS_ENABLED=true" -e "GF_AUTH_ANONYMOUS_ORG_NAME=iaa" -e "GF_SERVER_ROOT_URL=http://dev-sicg.labtech.pls.renault.fr:3000" -e "GF_SECURITY_ADMIN_PASSWORD=mastersicg" -p 3000:3000 --name grafana --restart always --dns 172.26.80.129 grafana/grafana:6.4.3
