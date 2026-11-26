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
   verificado" é esperado — NÃO enviar para verificação). ~~Passo 8~~ **Feito.** → **Próximo: Fase 3.**
5. **Fase 3** — mudar visibilidade (videos.update part=status — mandar o objeto status inteiro para não
   zerar madeForKids/embeddable etc.), comentários (listar/responder/moderar).
6. **Fase 4** — e-mail (Gmail API). 7. Ícone do usuário (`Studio_Movel_Icone_120x120.png`) como ícone do app.

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
