# HANDOFF — Studio Móvel

## Objetivo atual
App Android de uso próprio para gerenciar **6 canais do YouTube** (uma conta Google por canal, número vai
crescer) e o **e-mail dessas contas**, sem trocar de conta no celular.

Canais: JBVoid · Não Mais que 3 Minutos · Ovelha que Voa · Cordeiro de Luz · E se, Teca? · JB-Vortex

**Excelente =** tela inicial com todos os canais; tocar no canal → vídeos com visibilidade, views e
comentários novos; responder comentário ali; mudar não listado → público em 2 toques; aba de e-mail
unificada mostrando de qual conta veio; adicionar canal novo = "Entrar com Google" de novo; login uma
vez só por conta; simples, em português, usável com uma mão.

## Estado atual (2026-11-25)
- **Fase 1 feita:** Google Cloud com projeto, APIs, consentimento Externo em "Testando", 8 usuários de teste, sem logo.
- **Fase 2 — APK compila no GitHub Actions ✅** (release v0.2.6, `StudioMovel.apk` ~7,9 MB). **Testado pelo usuário no celular: instalou, adicionou os canais, tudo ok.**
  Link fixo: https://github.com/jbalvezz/studio-movel/releases/latest/download/StudioMovel.apk
  - Projeto Kotlin/Compose em `app/` (pacote `com.studiomovel.app`, minSdk 26, AGP 8.7.3, Kotlin 2.0.21, Gradle 8.11.1 no CI).
  - `Autorizador.kt` (seletor de conta do Android + AuthorizationClient por conta), `YouTubeApi.kt`
    (canal + últimos 50 vídeos com visibilidade/views/comentários), `Contas.kt` (lista no SharedPreferences),
    `MainActivity.kt` (tela Meus canais + tela de vídeos com filtros; tocar abre no YouTube Studio).
  - `.github/workflows/apk.yml` gera APK assinado e publica em Releases (`StudioMovel.apk`).
  - `docs/` = página inicial + política de privacidade (GitHub Pages).
  - Chave de assinatura gerada (PKCS12) em `chave-assinatura/` — **fora do git**. SHA-1 em `chave-assinatura/SHA1.txt`.
- Instalado no PC (com OK do usuário): pacote Python `cryptography` (--user).

## Pendências (ordem)
1. **Usuário:** passos 4–5 do guia (cliente Android com SHA-1; criar repo público `studio-movel` vazio) e informar o usuário do GitHub.
2. ~~Commit + push~~ **Feito**: https://github.com/jbalvezz/studio-movel (branch `main`). Push pelo
   terminal do app (o Git Credential Manager abre o navegador; o Bash não-interativo não consegue).
   Status das builds (repo público): `https://api.github.com/repos/jbalvezz/studio-movel/actions/runs`.
3. ~~Passo 6 (segredos)~~ **Feito.** Se uma build falhar, o erro do Kotlin/Gradle aparece como anotação pública:
   `api.github.com/repos/jbalvezz/studio-movel/check-runs/<job_id>/annotations` (logs exigem login).
4. ~~Passo 7~~ **Feito: app "Em produção"** (não verificado, limite 100 usuários; aviso amarelo "precisa ser
   verificado" é esperado — NÃO enviar para verificação). ~~Passo 8~~ **Feito.** → Fase 3 feita (item 9).
5. **Fase 3** — mudar visibilidade (videos.update part=status — mandar o objeto status inteiro para não
   zerar madeForKids/embeddable etc.), comentários (listar/responder/moderar).
6. **Fase 4** — e-mail (Gmail API).
7. **v0.3 (2026-11-25) — testada (ícone, fundo, organizar ok):**
   - Ícone do usuário (`Logo app/Studio_Movel_Vermelho_512x512.png`) → `mipmap-*/ic_launcher_foreground.png`
     (arte a 60/108 do quadro) sobre fundo radial azul-escuro.
   - Tema escuro próprio nas cores do ícone (ciano/roxo/vermelho), fundo em degradê com brilhos suaves
     e marca d'água do ícone (7% opacidade). Cores dinâmicas do Android foram desligadas.
   - "Organizar canais" (ícone de lápis na tela inicial): setas ↑↓ e remover; ordem salva em `Contas.mover`.
   - Botão Gmail (envelope vermelho) por canal e na tela de vídeos: abre `mail.google.com/mail/u/<email>/`
     no navegador. Atalho provisório até a caixa unificada (Fase 4).
   - Gmail pelo navegador NÃO funcionou (v0.3.8 app do Gmail travava; v0.3.9 `/u/<email>` dava 404 e
     `?authuser=` também falhou). Atalho do navegador foi REMOVIDO.
8. **v0.4 (2026-11-26) — e-mail dentro do app — TESTADA pelo usuário: funcionou.**
   - `Rede.kt` (GET/POST + ErroApi), `GmailApi.kt` (INBOX 25 por página com cabeçalhos em paralelo,
     abrir mensagem format=full, marcar como lido), `TelasEmail.kt` (TelaCaixa, TelaMensagem com WebView
     sem JavaScript; links abrem fora).
   - Envelope ✉️ no cartão do canal e na tela de vídeos abre a caixa daquele canal.
   - Falta na fase 4: responder, arquivar, buscar, contador de não lidos no cartão.
9. **v0.5 (2026-11-26) — Fase 3 — TESTADA pelo usuário: funcionou.**
   - Visibilidade: tocar no selo do vídeo (ou menu ⋮ → Mudar visibilidade) → Público / Não listado /
     Privado / Agendar (data+hora; vira private + publishAt). `YouTubeApi.mudarVisibilidade` lê o status
     atual e reenvia os campos graváveis (embeddable, license, publicStatsViewable, selfDeclaredMadeForKids,
     containsSyntheticMedia) para não zerar nada. Tela: `DialogoVisibilidade.kt`.
   - Comentários (`TelasComentarios.kt`): por vídeo (menu ⋮ → Comentários) ou do canal todo (ícone de
     sino na tela de vídeos, `allThreadsRelatedToChannelId`, com título do vídeo). Responder (comments.insert),
     Ocultar (setModerationStatus=heldForReview → vai para "Em análise" no Studio, reversível).
   - `Rede.kt` ganhou PUT e POST sem corpo. Cota: update/insert/moderar custam 50 cada (limite 10.000/dia).
   - Ainda não feito: editar título/descrição, curtir/fixar comentário, apagar a própria resposta.
10. **v0.6 (2026-11-26) — e-mail: responder/arquivar/lixeira — TESTADA pelo usuário: funcionou (respostas entre contas chegam certo).**
   - `TelaMensagem` ganhou barra embaixo: Responder (diálogo; `GmailApi.responder` monta MIME text/plain
     base64 com In-Reply-To/References e threadId → messages/send, cai na mesma conversa), Arquivar
     (remove INBOX), Lixeira (messages/trash, com confirmação; recuperável por 30 dias).
   - Contador de não lidos (labels/INBOX threadsUnread) como selo no envelope do cartão do canal.
     Usa `aut.token(email, silencioso = true)` para nunca abrir janela de permissão na tela inicial.
   - Lição: o Bash tool transforma `\r\n` dentro de heredoc em quebra real → em Kotlin usei
     `13.toChar() + 10.toChar()`. Scripts Python grandes: escrever com Write no scratchpad e rodar.
   - Ainda não feito: buscar e-mails, escrever e-mail novo, anexos.
11. **v0.7 (2026-11-26) — AGUARDANDO TESTE do usuário.**
   - Caixa de entrada: segurar o dedo num e-mail entra no modo seleção (toque marca/desmarca, "Todos",
     Arquivar = messages/batchModify; Lixeira = messages/trash em paralelo, com confirmação).
   - Resposta a e-mail enviado pela própria conta vai para o destinatário original (`GmailApi.destinoResposta`).
   - Vídeos: menu ⋮ → "Editar título e descrição" (`DialogoEditar.kt`). `YouTubeApi.editarTextos` lê o
     snippet atual e reenvia categoryId/tags/idiomas (senão o YouTube apaga). Limites: título 100, descrição
     5000 bytes, sem < >.
   - Ideias futuras: buscar e-mails, escrever e-mail novo, curtir/fixar comentário, editar tags/miniatura.
- Chave: usuário fez backup em pendrive. Recomendado manter também uma 2ª cópia (Drive particular).

## Decisões técnicas (podem ser revistas)
- **Kotlin + Jetpack Compose**, app nativo. APK gerado no **GitHub Actions** → usuário não precisa
  instalar Android Studio. Pedir OK antes de instalar qualquer coisa no PC (ex.: JDK/git, se precisar).
- **Multi-conta:** Google Identity Services `AuthorizationClient` com `setAccount(...)` por conta — o
  Play Services guarda a autorização e renova tokens em silêncio; sem servidor próprio. Plano B: AppAuth.
- Escopos: `youtube.force-ssl`, `gmail.modify`. App de consentimento **Externo, publicado sem
  verificação** (evita expiração de 7 dias do modo Teste; limite de 100 usuários, aviso "não verificado").
- Cota YouTube: 10.000 unidades/dia por projeto (list = 1, update/insert = 50). Suficiente para uso próprio;
  usar cache local para economizar.

## Limitações conhecidas (a API do YouTube NÃO permite)
- "Vídeo relacionado" dos Shorts, telas finais e cards → só pelo YouTube Studio (app abre o link direto).

## O que falhou
- **Publicar app bloqueado** (botão apagado: "conclua a configuração na página de branding"). Provável
  causa: faltam links de página inicial / política de privacidade. Decisão: ficar em **"Testando"** por
  enquanto (8 usuários de teste já cadastrados). Na fase 2, criar política de privacidade simples no
  **GitHub Pages** (repositório do app), preencher em Branding (sem logo!) e publicar. Até lá, pode ser
  preciso reconectar contas a cada 7 dias.

## Decisões do usuário
- Uso exclusivo próprio por enquanto; publicar na Play Store talvez "bem depois".
- Usuário iniciante: guiar passo a passo, em português do Brasil, sem jargão.
- Tem conta no GitHub (pode criar repositório novo para o app).

## Arquivos importantes
- `GUIA-GOOGLE-CLOUD.md` — passo a passo do Google Cloud para o usuário.
- `HANDOFF.md` — este arquivo.

> Nunca colocar senhas, tokens, client secrets ou keystore/senha da keystore aqui nem no GitHub.
