# Aplicativo de Conversas Instantâneas com Sockets

## Descrição

Este projeto implementa um aplicativo de conversas instantâneas utilizando sockets em Java. A aplicação permite que múltiplos clientes se conectem a um servidor central para trocar mensagens de texto e arquivos de forma eficiente e segura.

## Funcionalidades

- **Comunicação Cliente-Servidor:** Todos os clientes se comunicam através do servidor, que atua como intermediário para rotear mensagens e arquivos.
- **Mensagens Direcionadas:** Envie mensagens de texto diretamente para um destinatário específico.
- **Envio de Arquivos:** Transfira arquivos para outros clientes conectados.
- **Listar Usuários Conectados:** Utilize o comando `/users` para ver todos os usuários atualmente conectados.
- **Finalizar Comunicação:** Encerre a sessão a qualquer momento com o comando `/sair`.
- **Logs de Conexão:** O servidor mantém um log detalhado das conexões, incluindo endereço IP e data/hora.


## Como executar o projeto na sua máquina

### 1. Clonar o Repositório

```bash
git clone https://github.com/seu-usuario/seu-repositorio.git
cd seu-repositorio
```

### 2. Compilar os Fontes
Navegue até o diretório onde os arquivos .java estão localizados e compile-os:
```bash
javac Server.java
javac Client.java
```
### 3. Iniciar o Servidor antes
Execute o servidor antes de iniciar os clientes:
```bash
java Server
```
O servidor estará rodando na porta 12345.

### 4. Iniciar os Clientes
Em terminais separados, execute o cliente:
```bash
java Client
```
### Observações:
- Ao iniciar, o cliente solicitará que você insira um nome de usuário.
- É possível iniciar várias instâncias de clientes com nomes de usuários diferentes para testar em apenas uma máquina.
- Se os clientes estiverem em computadores diferentes, o endereço IP está definido como localhost e deve ser substituído pelo IP da máquina que está executando o servidor.

## Comandos Disponíveis

```bash
/send message <destinatario> <mensagem>
/send file <destinatario> <caminho do arquivo>
/users
/sair
```

## Logs de Conexão
O servidor mantém um arquivo de log chamado server_log que registra:

- **Nome do cliente**
- **Endereço IP**
- **Data e hora da conexão**
- **Mensagens enviadas entre os clientes**
- **Este arquivo é atualizado automaticamente conforme os clientes se conectam e interagem**
