## JK Status Exporter

Um projeto simples em que faz o parse do arquivo gerado pela página do JKStatus, em formato XML para o formato do Prometheus.

Não foi utilizado a lib do prometheus client.

O projeto possui um Dockerfile simples, e uma imagem no Dockerhub, para fazer uso, utilize o seguinte formato de arquivo _config.yml_

```yaml
remote:
  url: https://remote/
  context: jkstatus
  login: admin
  pass: secret

local:
  url: http://127.0.0.1/
  context: jkstatus
```

Para adicionar em um arquivo _docker-compose.yml_ utilize o seguinte formato de exemplo:

```docker
jkstatus-exporter:
    image: romulofc/jkstatus_exporter
    container_name: jkstatus-exporter
    volumes:
    - ./config.yml:/etc/jkstatus_exporter/config.yml:ro
    ports: 
    - 9573:9573
```