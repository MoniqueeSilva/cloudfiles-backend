# CloudFiles — Backend

API REST responsável por receber arquivos, validar as informações, armazenar os objetos no Amazon S3 e disponibilizar os links de acesso por meio do Amazon CloudFront.

## 1. Visão geral do projeto

O CloudFiles é uma aplicação de armazenamento e disponibilização de arquivos na nuvem. O backend foi desenvolvido em Java com Spring Boot e utiliza serviços da AWS para armazenar e distribuir os arquivos.

### Tecnologias e serviços

- **Java 21** — linguagem e ambiente de execução.
- **Spring Boot** — estrutura da aplicação backend e exposição da API REST.
- **Maven** — gerenciamento de dependências e execução do projeto.
- **Amazon EC2 (Elastic Compute Cloud)** — máquina virtual na nuvem utilizada para executar o backend.
- **Amazon S3 (Simple Storage Service)** — armazenamento dos arquivos enviados.
- **Amazon CloudFront** — distribuição e entrega dos arquivos armazenados no S3.
- **AWS SDK for Java** — comunicação do backend com os serviços da AWS.

## 2. Arquitetura e papel de cada serviço AWS

O fluxo geral da aplicação é:

1. O usuário seleciona um arquivo na interface frontend.
2. O frontend envia o arquivo para a API backend por uma requisição HTTP `POST`.
3. O backend recebe o arquivo, verifica se ele atende às regras de validação e gera uma chave/identificador para o objeto.
4. O backend envia o arquivo ao bucket do Amazon S3.
5. Após o armazenamento, o backend monta a URL pública de distribuição do arquivo usando o domínio do CloudFront.
6. O backend devolve os dados do arquivo em uma resposta JSON.
7. Quando alguém acessa a URL retornada, o CloudFront entrega o arquivo a partir do S3, respeitando as permissões configuradas.

### Por que utilizamos o Amazon EC2?

O **Amazon EC2** fornece um servidor virtual na nuvem para executar a aplicação Spring Boot. Em vez de depender exclusivamente de um computador local para iniciar a API, podemos hospedar o backend em uma instância EC2 e permitir que o frontend ou outros clientes acessem a API por meio de um endereço de rede configurado para essa instância.

No projeto, o EC2 tem a função de **hospedar e executar o backend/API**. Ele não é o serviço responsável por guardar os arquivos enviados. Essa responsabilidade é do S3.

O uso do EC2 permite:

- manter a API executando em um ambiente de servidor na nuvem;
- disponibilizar os endpoints para acesso remoto, conforme as regras de rede e segurança configuradas;
- separar a camada de processamento (backend) da camada de armazenamento (S3);
- demonstrar o provisionamento e a implantação de uma aplicação Java em uma infraestrutura AWS.

A instância precisa ter Java compatível, aplicação configurada e regras de rede adequadas. A porta usada pela API neste projeto em desenvolvimento é `8081`; em uma implantação, a exposição dessa porta deve seguir a configuração de segurança adotada. Não inclua IP público, DNS, chaves ou outros dados de acesso neste README.

### Por que utilizamos o Amazon S3?

O S3 é o armazenamento de objetos da aplicação. Os arquivos enviados são guardados em um bucket, sem que o backend precise manter os conteúdos em seu próprio disco.

Benefícios no projeto:

- armazenamento durável e escalável;
- separação entre processamento e armazenamento;
- integração direta com outros serviços AWS;
- possibilidade de manter o bucket privado e controlar o acesso por políticas.

### Por que utilizamos o Amazon CloudFront?

O CloudFront é a rede de distribuição de conteúdo (CDN) usada para entregar os arquivos armazenados no S3. O backend retorna uma URL do domínio CloudFront, em vez de expor diretamente uma URL do bucket.

Benefícios:

- distribuição de conteúdo por uma CDN;
- possibilidade de reduzir a latência de acesso;
- camada de distribuição separada do backend;
- integração com o S3 como origem.

Neste projeto, o acesso do CloudFront ao bucket é configurado por meio de **Origin Access Control (OAC)**. Assim, o bucket pode permanecer privado, permitindo que a distribuição CloudFront acesse os objetos conforme a política configurada.

## 3. Requisitos

Para executar o backend localmente, tenha instalado:

- Java JDK 21;
- Maven (ou utilize o Maven Wrapper, se estiver disponível no repositório);
- Git;
- credenciais AWS com permissões estritamente necessárias para a operação no bucket configurado.

A aplicação também precisa dos valores de configuração do bucket S3 e do domínio CloudFront.

## 4. Configuração do ambiente

### 4.1. Clone o repositório

```bash
git clone [https://github.com/MoniqueeSilva/cloudfiles-backend.git]
cd cloudfiles-backend
```

### 4.2. Configure as variáveis de ambiente

Crie um arquivo `.env` na raiz do projeto, conforme o mecanismo de carregamento configurado no `application.properties`:

```env
AWS_ACCESS_KEY_ID=sua_access_key
AWS_SECRET_ACCESS_KEY=sua_secret_key
AWS_REGION=us-east-2
```

Em ambientes AWS, prefira uma **IAM Role** associada à instância EC2, com permissões mínimas necessárias para acessar o bucket. Dessa forma, evita-se armazenar chaves de acesso permanentes na máquina.

### 4.3. Confira as propriedades da aplicação

No `src/main/resources/application.properties`, configure os valores correspondentes ao ambiente:

```properties
server.port=8081

aws.bucketName=SEU_BUCKET_S3
aws.region=us-east-2
cloudfront.domain=SEU_DOMINIO_CLOUDFRONT
```

Os nomes exatos das propriedades devem corresponder aos utilizados pelas classes de configuração do projeto. Não substitua os valores por credenciais. O domínio CloudFront deve ser informado sem inventar um endereço: use o domínio da distribuição criada para o ambiente.

## 5. Executar localmente

Na raiz do backend:

```bash
mvn spring-boot:run
```

A API local fica disponível em:

```text
http://localhost:8081
```

## 6. Endpoints e requisições disponíveis

Atualmente, os endpoints funcionais documentados para arquivos são os seguintes:

| Método HTTP | Endpoint | Finalidade |
|---|---|---|
| `GET` | `/api/arquivos` | Listar os arquivos armazenados e seus metadados/links |
| `POST` | `/api/arquivos` | Enviar um novo arquivo para armazenamento |

### 6.1. GET — listar arquivos

**Requisição**

```http
GET http://localhost:8081/api/arquivos
```

Não é necessário enviar corpo (`Body`) para essa requisição.

**Resposta esperada**

Status `200 OK`, com uma lista JSON. Cada item pode conter:

```json
[
  {
    "id": "identificador-do-objeto.png",
    "nome": "imagem.png",
    "url": "https://DOMINIO_CLOUDFRONT/identificador-do-objeto.png",
    "tamanho": 35839,
    "tipo": "image/png"
  }
]
```

### 6.2. POST — enviar arquivo

**Requisição**

```http
POST http://localhost:8081/api/arquivos
```

No Postman:

1. Selecione o método `POST`.
2. Abra **Body**.
3. Selecione **form-data**.
4. Crie uma única linha com a chave `arquivo`.
5. Altere o tipo da chave de `Text` para `File`.
6. Selecione o arquivo do computador e envie.

| Campo | Tipo | Obrigatório |
|---|---|---|
| `arquivo` | File | Sim |

Não envie o arquivo como JSON ou como texto. O endpoint espera `multipart/form-data`.

**Resposta de sucesso**

O envio retorna `201 Created` e um JSON com os dados do arquivo, por exemplo:

```json
{
  "id": "identificador-gerado.pdf",
  "nome": "arquivo.pdf",
  "url": "https://DOMINIO_CLOUDFRONT/identificador-gerado.pdf",
  "tamanho": 28562,
  "tipo": "application/pdf"
}
```

## 7. Regras de validação do upload

O backend valida o arquivo antes de armazená-lo.

Formatos aceitos:

- PDF (`.pdf`);
- JPEG (`.jpg` e `.jpeg`);
- PNG (`.png`);
- Microsoft Word (`.doc` e `.docx`).

Limite de tamanho:

- máximo de **10 MB** por arquivo.

Exemplos de situações inválidas testadas:

- requisição sem o campo/arquivo esperado;
- tipo de arquivo não permitido;
- arquivo acima do limite de tamanho.

As validações evitam que arquivos fora das regras sejam armazenados. A mensagem e o status HTTP retornados dependem do tipo de erro identificado.

## 8. Integração com o frontend

O frontend React/Vite consome a API por meio da URL base:

```text
http://localhost:8081/api/arquivos
```

Em desenvolvimento local:

- frontend: `http://localhost:5173`;
- backend: `http://localhost:8081`.

A configuração de CORS do backend permite a origem local do frontend e os métodos usados pela aplicação (`GET`, `POST` e `OPTIONS`). Em produção, a origem permitida deve ser ajustada para o domínio real do frontend, evitando liberar origens desnecessárias.

Quando o backend estiver hospedado na EC2, o frontend deverá apontar para o endereço público/DNS ou domínio configurado para a API, e não para `localhost`. O valor deve ser configurado conforme o ambiente de implantação.

## 9. Testes

Para validar a API teste:

1. `GET /api/arquivos` — verificar se a listagem retorna `200 OK`.
2. `POST /api/arquivos` com arquivo permitido — verificar `201 Created`.
3. `GET /api/arquivos` novamente — confirmar que o novo arquivo aparece na listagem.
4. `POST /api/arquivos` sem selecionar arquivo — verificar `400 Bad Request`.
5. `POST /api/arquivos` com `.txt` — verificar rejeição do formato.
6. `POST /api/arquivos` com arquivo maior que 10 MB — verificar rejeição pelo limite de tamanho.
7. Abrir a URL CloudFront devolvida no POST ou GET — confirmar que o arquivo é entregue.
