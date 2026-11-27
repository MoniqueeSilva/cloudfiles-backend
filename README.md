## CloudFiles Backend

**Dupla:** Júlio Soares da Silva e Monique da Silva Nascimento  
**Disciplina:** Sistemas Distribuídos  

## Sobre o Projeto

O **CloudFiles** é uma aplicação de armazenamento de arquivos na nuvem. Este repositório contém o **backend** da aplicação, desenvolvido em **Java (Spring Boot)**, que expõe uma API REST para receber arquivos, armazená-los no **Amazon S3** e disponibilizá-los via **Amazon CloudFront**. O backend é hospedado em uma instância **Amazon EC2**, demonstrando a integração de serviços de computação, armazenamento e distribuição de conteúdo da AWS.

A aplicação foi projetada para ser simples e funcional, servindo como demonstração prática dos conceitos de sistemas distribuídos e computação em nuvem.

## Tecnologias e Ferramentas

- **Java 21** e **Spring Boot** para a API REST.
- **Maven** para gerenciamento de dependências e build.
- **AWS SDK for Java (S3)** para integração com o Amazon S3.
- **Amazon EC2** (t3.micro) para hospedagem do backend.
- **Amazon S3** para armazenamento dos arquivos.
- **Amazon CloudFront** para distribuição e acesso aos arquivos.
- **Insomnia** para testes da API.

## Arquitetura

A arquitetura do sistema segue o fluxo:

```
Cliente (Frontend) -> API (EC2) -> Amazon S3 -> Amazon CloudFront -> Cliente
```

1.  O frontend envia um arquivo via `POST` para a API.
2.  A API (rodando na EC2) recebe o arquivo e o envia para o bucket S3.
3.  O S3 armazena o arquivo.
4.  A API retorna a URL do CloudFront para o cliente, que pode acessar o arquivo publicamente.

## Configuração da AWS

Para executar este projeto, é necessário ter uma conta na AWS com os seguintes recursos configurados:

1.  **Bucket S3:** Um bucket privado (ex: `cloudfiles-backend-julio`) na região `us-east-2`.
2.  **Usuário IAM:** Um usuário com permissões de acesso ao S3 (Access Key e Secret Key).
3.  **Instância EC2:** Uma instância (ex: t3.micro) rodando Ubuntu 24.04, com o Java instalado e a porta 8081 liberada no Security Group.
4.  **CloudFront:** Uma distribuição configurada com o bucket S3 como origem.

## Clonando e executando o projeto localmente

```bash
git clone https://github.com/MoniqueeSilva/cloudfiles-backend
cd cloudfiles-backend
```

### Pré-requisitos: Variáveis de Ambiente

Crie um arquivo `.env` na raiz do projeto com as credenciais da AWS:

```env
AWS_ACCESS_KEY_ID=sua_access_key
AWS_SECRET_ACCESS_KEY=sua_secret_key
AWS_REGION=us-east-2
CLOUDFRONT_DOMAIN=seu_dominio_cloudfront
```

### Executando a aplicação

```bash
mvn spring-boot:run
```

A API estará disponível em `http://localhost:8081`.

## Endpoints da API

| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `GET` | `/health` | Verifica se a API está rodando. |
| `POST` | `/api/arquivos` | Envia um arquivo (Multipart Form). |
| `GET` | `/api/arquivos` | Lista todos os arquivos enviados. |

### Exemplo de Resposta (POST /api/arquivos)

```json
{
  "id": "uuid-gerado",
  "nome": "nome-original.jpg",
  "url": "https://d36r9035wj7gri.cloudfront.net/uuid.jpg",
  "tamanho": 1024,
  "tipo": "image/jpeg"
}
```

## Como testar a API (Passo a Passo)

### Teste 1: Verificar a saúde da API

```bash
curl http://localhost:8081/health
```
*Resposta esperada: `API CloudFiles está rodando!`*

### Teste 2: Enviar um arquivo (Upload)

No **Insomnia** (ou Postman):
1.  Crie uma requisição `POST` para `http://localhost:8081/api/arquivos`.
2.  No **Body**, escolha `Multipart Form`.
3.  Adicione o campo `arquivo` (tipo File) e selecione uma imagem ou PDF.
4.  Clique em **Send**.
*Resposta esperada: Status `201 Created` com o JSON contendo a URL do CloudFront.*

### Teste 3: Listar os arquivos enviados

No **Insomnia**:
1.  Crie uma requisição `GET` para `http://localhost:8081/api/arquivos`.
2.  Clique em **Send**.
*Resposta esperada: Status `200 OK` com a lista de arquivos em JSON.*

### Teste 4: Acessar o arquivo via CloudFront

Copie a `url` retornada no teste 2 e cole no navegador. O arquivo será exibido.

## Implantação na EC2 (Produção)

Para rodar o backend na EC2, siga os passos:

1.  **Gerar o pacote:**
    ```bash
    mvn clean package
    ```
2.  **Transferir o `.jar` para a EC2:**
    ```bash
    scp -i ~/Downloads/cloudfiles-key.pem target/api-0.0.1-SNAPSHOT.jar ubuntu@<IP_PUBLICO>:~/
    ```
3.  **Conectar na EC2:**
    ```bash
    ssh -i ~/Downloads/cloudfiles-key.pem ubuntu@<IP_PUBLICO>
    ```
4.  **Criar o `.env` na EC2** com as credenciais da AWS.
5.  **Executar a API em segundo plano:**
    ```bash
    set -a; source .env; set +a; nohup java -jar api-0.0.1-SNAPSHOT.jar > log.txt 2>&1 &
    ```
6.  **Verificar se subiu:**
    ```bash
    cat log.txt
    ```

## Estrutura do Projeto

```
cloudfiles-backend/
├── src/
│   ├── main/
│   │   ├── java/com/cloudfiles/api/
│   │   │   ├── config/       # Configuração do S3 e CORS
│   │   │   ├── controllers/  # Endpoints da API
│   │   │   ├── dtos/         # Objetos de transferência de dados
│   │   │   ├── exceptions/   # Tratamento de erros
│   │   │   └── services/     # Lógica de negócio (S3Service)
│   │   └── resources/
│   │       └── application.properties
│   └── test/                 # Testes unitários
├── .env                      # Variáveis de ambiente (local)
├── .env.example              # Exemplo de variáveis
├── pom.xml                   # Dependências Maven
└── README.md                 # Documentação
```