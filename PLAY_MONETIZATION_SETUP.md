# Monetização: configuração para publicação

## Estado desta candidata

| Fluxo | Estado atual |
| --- | --- |
| AdMob premiado | SDK/UMP reais; IDs Google de teste no debug, unidade própria no release. Recompensa apenas no callback do SDK, com recibo local transacional. |
| VIP gratuito e licença gratuita | Demonstrações restritas ao debug. O VIP demo não concede mais as 1.000 gemas iniciais. |
| 300 gemas pela licença | Troca local com moeda do jogo, sem dinheiro real. |
| Pacotes de gemas por créditos | Trocas locais com créditos do jogo, sem Play Billing. |
| VIP Play | Produto de assinatura previsto; ID e servidor verificador ainda ausentes. Checkout desabilitado. |
| Licença Play | Produto único não consumível previsto; ID e servidor verificador ainda ausentes. Checkout desabilitado. |
| Guilda, ranking, evento | Simulações/gameplay locais; rótulos de demonstração/local na UI. |
| Backup | Firebase real quando configurado; sandbox somente debug. A flag VIP restaurada não vale como compra em release. |

## Responsáveis e configuração externa

- **Titular da Play Console:** cadastrar a assinatura VIP e o produto único não consumível da licença; fornecer os IDs exatos, planos/ofertas, país/preço e contas de teste. Não há produto consumível pago nesta interface. Por isso o código faz acknowledgement de VIP e licença; não há consumo de token a fazer.
- **Desenvolvedor/backend:** publicar um serviço HTTPS autenticado por Firebase ID token que verifique `purchaseToken`, `productId`, pacote e titular usando Google Play Developer API, controle elegibilidade, expiração, cancelamento/revogação, notificações RTDN e concessão única do presente VIP. A resposta esperada pelo cliente é `{"productId":"<id>","active":true|false,"firstVipGift":true|false}`. `firstVipGift` só pode ser verdadeiro uma vez por conta, inclusive após reinstalação. O serviço precisa armazenar tokens e não confiar em saldos ou flags enviados pelo app.
- **Desenvolvedor/build:** após implantar e testar o servidor, fornecer `PLAY_PURCHASE_VERIFICATION_URL`, `PLAY_VIP_SUBSCRIPTION_ID` e `PLAY_AUTOSELL_PRODUCT_ID` como propriedades Gradle privadas. Enquanto qualquer parte faltar, o produto correspondente permanece indisponível. Não incluir credenciais de serviço na aplicação ou no Git.
- **Titular do AdMob:** revisar unidade premiada, aprovação do aplicativo, política de privacidade e parâmetros de público/consentimento. O sucesso em anúncio de teste não prova veiculação em produção.

## Limites de segurança

O cliente só aplica uma compra com resposta positiva do servidor e só faz acknowledgement depois de gravar o benefício. Recibos locais guardam apenas hash SHA-256 do token para evitar benefício repetido no mesmo banco. Esse recibo **não substitui** a verificação no servidor nem resolve reinstalações sozinho. A assinatura é reconciliada ao retomar: compra ausente, pendente ou sem confirmação ativa perde o benefício. Licença de origem Play perde o benefício quando não consta mais como ativa; licença de origem gemas permanece. Falha de consulta não autoriza benefício novo.

O backup v5 registra a origem da licença. Backups v2–v4 continuam aceitos, mas uma licença preexistente é marcada `LEGACY`: não há dado antigo suficiente para distinguir compra por gemas de ativação demo. Para não apagar compras locais legítimas, essa origem é preservada e precisa de revisão antes da publicação. Flags VIP antigas são desativadas em release até reconciliação com Play/backend.

Não abrir cobrança real antes de o serviço verificador, os produtos e o teste de ponta a ponta existirem. A build que apenas compila o Billing SDK ainda não está pronta para venda.
