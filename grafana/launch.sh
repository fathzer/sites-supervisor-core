#An example of how to launch grafana with Docker
ID=$(id -u)
docker run -d --user $ID -v "/grafana/data:/var/lib/grafana" -e "GF_AUTH_ANONYMOUS_ENABLED=true" -e "GF_AUTH_ANONYMOUS_ORG_NAME=mycompany" -e "GF_SERVER_ROOT_URL=http://grafana.mycompany.com:3000" -e "GF_SECURITY_ADMIN_PASSWORD=pwd" -p 3000:3000 --name grafana --restart always grafana/grafana:6.4.3
