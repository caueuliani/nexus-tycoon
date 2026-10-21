# Etapa 3B — exclusão de conta e backup (procedimento proposto)

**Estado: proposta operacional, NÃO homologada nem publicada.** Nenhuma conta real, backup real ou configuração Firebase de produção foi alterada nesta etapa. Não atualizar a política de privacidade para afirmar que a exclusão funciona até executar o ensaio integral abaixo.

## Escopo confirmado pelo código

- `CloudSaveManager.signIn()` troca credencial Google por sessão Firebase Auth; a UI exibe o e-mail. `logout()` encerra sessão e limpa o estado do provedor, mas não exclui Auth, Firestore ou Room.
- Backup manual escreve um único documento em `users/{uid}` por `.set(payload)`; restauração lê o mesmo caminho do servidor. O backup v7 contém progresso, pesquisadores, recursos, missões e recibos de anúncio. O código não grava e-mail dentro do documento. O jogo também funciona sem login.
- As regras versionadas autorizam o UID dono a obter, gravar e excluir seu documento, negando outro UID e listagem. **Não foi confirmado que são as regras implantadas no Firebase de produção.**
- Não há botão de exclusão de conta/backup nem backend administrativo no app. O Android declara backup automático com regras ainda genéricas; a exclusão do documento Firestore não apaga cópias locais ou backup do sistema.

## Caminho mínimo proposto: atendimento manual

**Responsável:** titular da conta Firebase/DevTecs ou pessoa formalmente autorizada por ele, com acesso administrativo individual, MFA e projeto correto. Canal de entrada: `contatodevtecs@gmail.com`. Não solicitar senha, token, código de login Google, documento de identidade ou backup completo por e-mail. E-mail de origem sozinho não comprova titularidade; não divulgar existência da conta antes de verificar.

1. Registrar pedido e escopo solicitado (conta Firebase, backup remoto, dados locais). Confirmar ao solicitante que a exclusão remota não apaga o progresso guardado em seu aparelho. Evitar copiar dados de jogo para a caixa postal.
2. Localizar no Firebase Authentication a conta Google pelo e-mail indicado, sem divulgar o resultado ainda. Confirmar que há uma associação única e conferir o UID e o projeto; se houver divergência ou conta inacessível, parar e escalar para avaliação individual.
3. Enviar desafio de uso único **para o e-mail que consta no registro Auth**, por mensagem nova originada do canal oficial. Exigir resposta com o código e confirmação explícita do escopo; definir validade curta e impedir reutilização. Essa verificação prova acesso à caixa postal, não substitui avaliação em casos de conta comprometida ou e-mail alterado. Nunca pedir credenciais. Um fluxo de prova de sessão no próprio aplicativo seria mais forte, mas exigiria implementação e teste.
4. Com titularidade confirmada, registrar internamente UID, projeto, operador e decisões, sem guardar o backup completo. Verificar se há subcoleções ou outros dados vinculados ao UID antes de apagar; o código atual só grava o documento direto, mas isso precisa ser conferido no ambiente real.
5. Executar a exclusão com acesso administrativo: remover `users/{uid}` no Firestore e o usuário correspondente no Firebase Authentication; conferir cada resultado pelo mesmo UID. Se uma etapa falhar, **não afirmar conclusão**: manter o caso aberto, corrigir a falha e repetir a verificação. Existe janela de corrida com sessão ainda ativa; repetir a checagem de ausência do documento após a exclusão da conta e documentar qualquer reaparecimento. Avaliar revogação de sessões/controle de concorrência no ensaio.
6. Responder ao solicitante apenas após confirmar ausência do documento e do usuário. Explicar que dados locais e possíveis backups Android dependem de ações do usuário no dispositivo/conta Google e não foram apagados remotamente por este procedimento. Não mandar exportação de progresso ou dados de outro usuário por e-mail.

O operador precisa definir antes do primeiro atendimento: controle de acesso, como gerar/registrar/destruir desafios, verificação de identidade em casos excepcionais, tratamento de falha parcial, registro mínimo da execução, eventual obrigação legal de conservação e prazo de resposta apropriado. Nenhum desses controles está operacionalmente confirmado. A revisão jurídica deve decidir se o atendimento manual é suficiente para a distribuição proposta.

## Ensaio necessário antes de declarar GO

Usar **projeto Firebase de teste separado**, conta Google descartável e save fictício, nunca o projeto de produção nem o POCO principal. Criar sessão e backup fictícios; pedir exclusão pelo canal; testar desafio, busca de UID, exclusão de documento e Auth, verificação de ausência, falha parcial e comunicação final. Confirmar que um segundo UID não é afetado e que a operação não apaga dados locais sem ação do usuário. Guardar evidência sem e-mail completo, UID completo, token ou conteúdo do backup. Se não houver projeto/conta isolados, o ensaio integral fica pendente.

## Evidência obtida nesta etapa

Em 20/10/2026, `node --test firebase-tests/rules.test.mjs` passou **10/10** contra emulador Firestore local `demo-nexus-rules`: dono fictício criou/excluiu backup; outro UID e visitante não puderam alterá-lo/excluí-lo. A primeira tentativa via Firebase CLI falhou por restrição de spawn de Java no sandbox; a execução direta do emulador com Java 21 e o teste Node passaram. O emulador foi encerrado. Isso **não** testa busca/exclusão administrativa de Firebase Auth, desafio por e-mail, regra de produção implantada, console, concorrência ou backup Android.

## Decisão específica deste fluxo

**NO-GO para afirmar que exclusão de conta e backup está atendida.** O procedimento manual é tecnicamente plausível sem backend pago ou mudança de gameplay, mas falta implementá-lo operacionalmente e homologá-lo ponta a ponta com conta descartável em projeto isolado. Não alterar a política pública nem publicar a variante até essa evidência existir. Caso o teste revele lacuna de verificação de titularidade ou corrida não controlável, o menor plano de código é uma ação autenticada de exclusão na área Perfil: confirmar, reautenticar Google quando exigido, excluir backup do UID e conta Auth, tratar falhas sem prometer sucesso e orientar sobre dados locais/backup Android. Esse plano deve ser revisado antes de qualquer implementação.

Referências: [Firebase Auth — gerenciamento e exclusão de usuário](https://firebase.google.com/docs/auth/admin/manage-users), [Firebase Auth Android — exclusão e login recente](https://firebase.google.com/docs/auth/android/manage-users), [Firestore — exclusão de documentos e subcoleções](https://firebase.google.com/docs/firestore/manage-data/delete-data), [ANPD — direitos do titular](https://www.gov.br/anpd/pt-br/assuntos/titular-de-dados-1/direito-dos-titulares).
