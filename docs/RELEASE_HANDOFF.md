# Nexus Tycoon — continuidade da beta itch.io

Estado em 23/10/2026. Este roteiro preserva a candidata existente; **não autoriza publicação**.

## Candidata preservada

| Item | Valor |
| --- | --- |
| Branch | `updates` |
| Commit do código usado na beta.2 | `e3da61ddf038685dfae40a4eddd8024863383213` |
| Variante e versão | `itchBeta`, `versionCode` 2, `versionName` `1.0-itch-beta.2` |
| `applicationId` | `com.aistudio.idletycoon.nxtyc` |
| APK assinado, local e fora do Git | `C:\Users\caueu\Documents\Codex\2026-09-26\bus\work\nexus-tycoon\app\build\outputs\apk\itchBeta\app-itchBeta.apk` |
| SHA-256 do APK | `8DB393B5C62B37791F6598559FCB20C2235B27F0A9CC555BAF0F7262F26B1C30` |
| SHA-256 público do certificado de assinatura | `FD:4D:2B:41:13:EF:F2:D7:8A:F6:A8:AE:45:6E:67:CE:CF:D2:FE:F6:42:1B:AF:6B:F0:14:86:F1:85:C5:3B:73` |

A beta.2 foi instalada por atualização no POCO e o proprietário relatou seis verificações manuais aprovadas: inicialização, progresso preservado, Loja legível, objetivos em português, três cápsulas disponíveis e N.O.V.A./demais telas funcionando. Isso não inclui ensaio de restauração em nuvem nem substitui revisão de publicação.

## Gerar e conferir uma atualização

O script local `C:\Users\caueu\Documents\Codex\2026-09-26\bus\work\Build-NexusItchBetaPrivate.ps1` usa a keystore permanente fora do repositório, alias `upload`, e pede a senha de forma oculta no PowerShell. Mantenha a cópia protegida da keystore e a senha em locais separados. Nunca cole a senha no chat, no Git ou em um comando que a imprima.

Antes de gerar outra versão, altere `versionCode` e o sufixo de `versionName` em `app/build.gradle.kts`, faça os testes afetados e confirme que a branch contém apenas as mudanças desejadas. Em um PowerShell local:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File 'C:\Users\caueu\Documents\Codex\2026-09-26\bus\work\Build-NexusItchBetaPrivate.ps1'
```

Para verificar um APK gerado, sem informar a senha:

```powershell
$apk = 'C:\Users\caueu\Documents\Codex\2026-09-26\bus\work\nexus-tycoon\app\build\outputs\apk\itchBeta\app-itchBeta.apk'
$sdk = 'C:\Users\caueu\Documents\Codex\2026-09-26\bus\work\toolchain\android-sdk\build-tools\36.0.0'
Get-FileHash -LiteralPath $apk -Algorithm SHA256
& "$sdk\apksigner.bat" verify --verbose --print-certs $apk
& "$sdk\aapt.exe" dump badging $apk | Select-String '^package:'
```

Confirme o SHA-256 esperado para a versão, o certificado permanente, `applicationId`, `versionCode` e `versionName`. Para uma versão nova o hash do APK muda; o certificado deve permanecer o mesmo. A variante deve continuar não depurável e sem anúncios, Billing ou ferramentas internas acessíveis. Não use chave debug nem crie outra keystore.

## Instalação por ADB sem apagar dados

Execute somente no dispositivo escolhido e com assinatura compatível. Se `adb devices -l` listar mais de um, identifique o serial correto. `install -r` atualiza mantendo dados; erro de assinatura exige investigação, **não** desinstalação automática.

```powershell
$adb = 'C:\Users\caueu\Documents\Codex\2026-09-26\bus\work\toolchain\android-sdk\platform-tools\adb.exe'
$apk = 'C:\Users\caueu\Documents\Codex\2026-09-26\bus\work\nexus-tycoon\app\build\outputs\apk\itchBeta\app-itchBeta.apk'
& $adb devices -l
& $adb -s '<SERIAL>' install -r $apk
& $adb -s '<SERIAL>' shell am start -n 'com.aistudio.idletycoon.nxtyc/com.example.MainActivity'
```

Para experiência de primeira instalação, use outro aparelho/perfil vazio ou dados explicitamente descartáveis. Não limpe o POCO por conveniência. Não instale a build pública sobre uma instalação debug com assinatura diferente.

## Firebase e testes essenciais

No Firebase Console, confira projeto `nexus-idle-tycoon` → app Android `com.aistudio.idletycoon.nxtyc`: SHA-1/SHA-256 da assinatura release, provedor Google habilitado e regras Firestore que limitam `users/{uid}` ao UID autenticado. Os fingerprints release foram cadastrados na Etapa 4B; revalide no Console se o login falhar. O texto das regras publicadas foi fornecido pelo proprietário; não trate o arquivo local, sozinho, como prova da implantação.

Roteiro curto, em conta e aparelho de QA controlados:

1. Abrir sem login, concluir tutorial e verificar N.O.V.A., Indústria, missões, navegação e Loja.
2. Fechar/reabrir e deixar o app em segundo plano; conferir progresso local e ganhos offline sem duplicação aparente.
3. Abrir as três opções de cápsula e uma revelação de carta quando houver recursos de teste, sem compra real.
4. Vincular Google, confirmar projeto/conta escolhidos e **verificar se já existe backup remoto antes de tocar em Salvar na Nuvem ou Carregar Nuvem**. Fazer upload/restauração somente com dados declarados descartáveis e após confirmar que nenhum backup existente será sobrescrito.
5. Conferir que a `itchBeta` não oferece anúncios, dinheiro real nem menus internos; inspecionar logcat por crash durante o roteiro.

Na beta.2, os seis testes manuais citados no início deste documento passaram segundo o proprietário. Backup/restauração, cápsula efetivamente aberta e verificação completa de logs não foram confirmados neste fechamento.

## Documentos e condições antes do itch.io

- Revisar juridicamente e publicar a [política de privacidade em rascunho](privacy-policy-itch-beta-draft.md) em URL pública estável; definir identificação empresarial, retenção e como comunicar alterações. Usar `contatodevtecs@gmail.com` como canal oficial somente com operação e teste documentados.
- Homologar o [runbook de exclusão](itch-beta-account-deletion-runbook-draft.md) pelo canal oficial, inclusive sessão ainda ativa e recuperação de falha parcial, com conta descartável e autorização específica. Logout não exclui conta, Firestore nem save local.
- Resolver a [revisão de avisos de terceiros](itch-beta-third-party-notices-review.md) para as dependências efetivamente empacotadas. Conservar o inventário de [assets visuais](itch-beta-visual-assets-inventory.md) e `SOUND_ASSETS.md`; pedir avaliação humana para nomes/retratos que evocam pessoas reais, especialmente pessoa viva.
- Na página itch.io, disponibilizar política estável, canal de suporte/exclusão, requisitos Android, instruções de instalação/atualização, caráter beta e avisos/atribuições exigidos. Não prometer compras, anúncios ou recursos ausentes. Publicar APK e política somente por decisão explícita do titular.

**Pendências efetivas para distribuição pública:** fluxo de exclusão ainda não homologado operacionalmente; política sem aprovação/URL; revisão de semelhanças pessoais e licenças/avisos; QA de backup/restauração com dados descartáveis; decisão final do titular sobre os riscos residuais. A candidata atual segue adequada apenas para QA privado.
