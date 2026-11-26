# Guia: preparar o Google Cloud para o Studio Móvel

Este é o "cadastro" que permite ao seu app conversar com o YouTube e o Gmail.
Leva uns 20–30 minutos. Faça **no computador**, com calma. Não custa nada (não precisa de cartão).

> Use **uma** das suas contas Google como "dona" do projeto (sugestão: a do JBVoid).
> As outras 5 contas só serão adicionadas como "usuários de teste".

---

## Passo 1 — Criar o projeto

1. Acesse **https://console.cloud.google.com** com a conta escolhida.
2. Se aparecerem termos de uso, leia e aceite.
3. No topo, clique no seletor de projeto (ao lado do logo "Google Cloud") → **Novo projeto**.
4. Nome do projeto: `Studio Movel` → **Criar**.
5. Espere alguns segundos e confira se o seletor no topo mostra **Studio Movel**.

## Passo 2 — Ativar as APIs (YouTube e Gmail)

1. Menu ☰ (canto superior esquerdo) → **APIs e serviços** → **Biblioteca**.
2. Pesquise **YouTube Data API v3** → abra → **Ativar**.
3. Volte à Biblioteca, pesquise **Gmail API** → abra → **Ativar**.

## Passo 3 — Tela de consentimento (a tela "Permitir acesso" que aparece no login)

1. Menu ☰ → **APIs e serviços** → **Tela de consentimento OAuth**
   (em algumas versões aparece como **Google Auth Platform** → **Branding**).
2. Clique em **Começar** / **Configurar**.
3. Preencha:
   - **Nome do app:** `Studio Móvel`
   - **E-mail de suporte:** o seu e-mail
   - **Público / Tipo de usuário:** **Externo**
   - **Dados de contato:** o seu e-mail
4. Aceite a política de dados do usuário e conclua.
5. ⚠️ **Não envie logotipo.** Com logo, o Google passa a exigir a verificação do app. Deixe também em
   branco os campos de domínio (página inicial, política de privacidade, termos, domínios autorizados).

### 3.1 — Permissões (escopos)
Em **Acesso a dados** (ou **Escopos**) → **Adicionar ou remover escopos**, marque:
- `https://www.googleapis.com/auth/youtube.force-ssl` (gerenciar vídeos e comentários)
- `https://www.googleapis.com/auth/gmail.modify` (ler, responder e organizar e-mails)

Se não achar na lista, cole o endereço no campo **"Adicionar escopos manualmente"**. Salve.

### 3.2 — Usuários de teste
Em **Público** (ou **Usuários de teste**) → **Adicionar usuários** → coloque os e-mails das
**6 contas** (inclusive a dona do projeto) → Salvar.

### 3.3 — Publicar (importante)
Ainda em **Público**, clique em **Publicar app** → confirme.
- Motivo: em modo "Teste", o Google **desconecta as contas a cada 7 dias**. Publicado, isso não acontece.
- **Não** precisa enviar para verificação. Para uso próprio (menos de 100 pessoas) o app funciona sem ela;
  só vai aparecer um aviso **"O Google não verificou este app"** no login — é normal, porque o app é seu.
  Basta tocar em **Avançado → Acessar Studio Móvel**.
- Se o Google não deixar publicar por algum motivo, deixe em Teste e anote o que apareceu — resolvemos na próxima sessão.

## Passo 4 — Credencial do Android (✅ pode fazer agora)

1. No Google Cloud: **Google Auth Platform → Clientes → Criar cliente**.
2. **Tipo de aplicativo:** Android. **Nome:** `Studio Móvel Android`.
3. **Nome do pacote:** `com.studiomovel.app`
4. **Impressão digital do certificado SHA-1:**
   `91:9B:75:DE:BF:C3:17:3D:3F:BD:C7:9D:AD:5C:4E:FB:C9:F4:47:31`
5. **Criar.** (Não precisa copiar nada dessa tela para mim.)

## Passo 5 — Repositório no GitHub

1. Entre em **https://github.com/new**.
2. **Repository name:** `studio-movel` · marque **Public** · **NÃO** marque "Add a README".
3. **Create repository** e me diga seu **nome de usuário do GitHub** — eu envio o código.
   (Na primeira vez, o Windows abre uma janela do GitHub pedindo para você entrar — é normal.)

## Passo 6 — Segredos da chave (no GitHub)

A pasta `chave-assinatura` (só no seu PC, **nunca** vai para o GitHub) tem dois arquivos de texto.
No repositório: **Settings → Secrets and variables → Actions → New repository secret**. Crie dois:

| Name | Secret (conteúdo) |
|---|---|
| `KEYSTORE_BASE64` | todo o texto de `chave-assinatura\KEYSTORE_BASE64.txt` |
| `KEYSTORE_PASSWORD` | todo o texto de `chave-assinatura\KEYSTORE_PASSWORD.txt` |

Depois: aba **Actions → Gerar APK → Run workflow**. Em ~5 minutos fica verde ✅.

> 💾 **Faça uma cópia da pasta `chave-assinatura`** (pendrive ou Google Drive particular).
> Sem ela, não dá para atualizar o app instalado no futuro.

## Passo 7 — Página de privacidade e publicar o app

1. No repositório: **Settings → Pages → Source: Deploy from a branch → Branch: `main` / pasta `/docs` → Save**.
   Em ~2 minutos a página fica em `https://jbalvezz.github.io/studio-movel/`.
2. No Google Cloud, **Branding** (sem logotipo!):
   - Página inicial: `https://jbalvezz.github.io/studio-movel/`
   - Política de privacidade: `https://jbalvezz.github.io/studio-movel/privacidade.html`
   - Domínios autorizados: `jbalvezz.github.io`
   - **Salvar**.
3. **Público-alvo → Publicar app**. Se ainda não deixar, mande um print.

## Passo 8 — Instalar no celular

1. No celular, abra `https://github.com/jbalvezz/studio-movel/releases/latest` e baixe **StudioMovel.apk**.
2. Abra o arquivo. O Android vai pedir para **permitir instalar apps desta fonte** (do navegador) — permita
   só para isso. Se aparecer o aviso do Play Protect, toque em **Instalar mesmo assim** (o app é seu).
3. Abra o **Studio Móvel → Adicionar canal** → escolha uma conta → aceite as permissões.
   Repita para cada canal. Se a conta não aparecer na lista, adicione-a antes em
   **Configurações do Android → Contas → Adicionar conta → Google**.

---

## Checklist
- [x] Passos 1–3 (projeto, APIs, consentimento, usuários de teste)
- [ ] Passo 4 — cliente Android com o SHA-1
- [ ] Passo 5 — repositório criado + me dizer o usuário do GitHub
- [ ] Passo 6 — dois segredos + Run workflow
- [ ] Passo 7 — Pages, Branding, publicar
- [ ] Passo 8 — instalar e adicionar os canais

> 🔒 Nunca me envie senhas. O conteúdo dos arquivos da pasta `chave-assinatura` vai **só** nos
> segredos do GitHub — não cole em chat nenhum.
