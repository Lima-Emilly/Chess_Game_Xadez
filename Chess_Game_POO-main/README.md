# My word purple | ChessGame

## 📖 Sobre o Projeto

O ChessGame é uma aplicação de xadrez desenvolvida em Java, com um tema visual único e marcante: **o roxo**. A interface foi meticulosamente desenhada para evocar a cor roxa em todas as suas tonalidades, da mais clara à mais escura, criando uma experiência de jogo imersiva e elegante.

O projeto foi construído para ser uma experiência de jogo clássica, mas com um toque visual distinto, substituindo as cores tradicionais do tabuleiro por uma paleta de roxos vibrantes. A interface gráfica é construída usando a biblioteca Swing, garantindo uma experiência interativa e amigável para todos os jogadores.

## Funcionalidades

  - **Tabuleiro Roxo Interativo:** Jogue xadrez clicando nas peças e nas casas, tudo com uma estética roxa.
  - **Temática Única:** Uma paleta de cores totalmente roxa para o tabuleiro e a interface.
  - **Histórico de Lances:** Acompanhe o progresso da partida em notação de xadrez.
  - **Partida contra a IA:** Enfrente um adversário controlado pelo computador com diferentes níveis de dificuldade.
  - **Indicação de Movimentos:** Realce de casas para indicar peças selecionadas, movimentos legais e o último lance.
  - **Promoção de Peão:** Caixa de diálogo para escolher a peça de promoção.

## Tecnologias

  - **Linguagem de Programação:** Java
  - **Biblioteca de UI:** Java Swing

## Como Rodar o Jogo

Siga estas instruções para compilar e executar o projeto.

### Pré-requisitos

  - JDK (Java Development Kit) 17 ou superior instalado.
  - Uma IDE como VS Code, IntelliJ IDEA ou Eclipse.

### Instruções

1.  **Clone o repositório:**

    ```bash
    git clone [https://github.com/seu-usuario/seu-repositorio.git](https://github.com/seu-usuario/seu-repositorio.git)
    cd seu-repositorio
    ```

2.  **Abra o projeto na sua IDE:**
    Importe a pasta do projeto para a sua IDE.

3.  **Compile e Execute:**

      - Certifique-se de que todas as dependências estejam configuradas corretamente.
      - Encontre o arquivo `ChessGUI.java` (localizado em `src/view/ChessGUI.java`).
      - Execute a classe `ChessGUI` para iniciar o jogo.

## Estrutura do Projeto

A estrutura do projeto está organizada da seguinte forma:

  - `src/controller/`: Lógica do jogo (como a IA e o controle da partida).
  - `src/model/`: Classes que representam o tabuleiro, as peças e as regras do jogo.
  - `src/view/`: Componentes da interface gráfica, incluindo a classe `ChessGUI`.