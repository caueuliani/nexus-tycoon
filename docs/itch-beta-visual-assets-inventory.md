# Etapa 3D — inventário visual da variante itchBeta

**Atualização técnica posterior (21/10/2026):** o commit `b0a7c93` substituiu as três referências de cápsulas da loja por ícones Material e tornou explícito o ícone fallback para IDs de pesquisadores fora dos 26 conhecidos. Os quatro PNGs inválidos ainda estão no Git, mas não são referenciados pelo código de produção. Esta tabela preserva o estado histórico da inspeção da Etapa 3D; consulte [itch-beta-release-gate.md](itch-beta-release-gate.md) para o checkpoint atual.

Estado em 21/10/2026: levantamento documental e inspeção visual, **não é liberação jurídica para distribuição**. Escopo: os 33 arquivos raster versionados em `app/src/main/res/`; nenhum foi substituído, removido ou alterado. `app/src/test/screenshots/greeting.png` é artefato de teste, não asset empacotado do jogo. Ícone do launcher, molduras e efeitos desenhados em XML/Compose são código, não arquivos raster adicionais.

## Origem e alcance da evidência

O proprietário declarou que **todas as imagens do jogo foram geradas por IA**, com Google AI Studio e ferramentas de geração do Codex/OpenAI, sem banco de imagens ou ilustrador externo contratado. Essa declaração esclarece o processo geral, mas não identifica a ferramenta, prompt, modelo, conta, data ou termos aceitos para cada arquivo. O README confirma origem do projeto no Google AI Studio, não a origem individual de cada imagem. O único registro individual encontrado é `docs/nova-pilot.md`, que identifica o holograma da N.O.V.A. como gerado pela ferramenta Codex em 18/10/2026. Os demais itens recebem a atribuição conservadora **“Google AI Studio ou OpenAI/Codex — origem individual não confirmada”**. Histórico Git de inclusão e ausência de metadados EXIF/XMP de geração não são comprovantes de autoria ou licença.

Nos caminhos da tabela, `drawable/` e `drawable-nodpi/` significam `app/src/main/res/drawable/` e `app/src/main/res/drawable-nodpi/`. A origem de todos os itens, exceto N.O.V.A., é a atribuição conservadora acima.

| Arquivo | Uso / observação objetiva | Integridade no checkout |
| --- | --- | --- |
| `drawable/img_sci_elon.png` | Pesquisador “Elon Dusk-V”; referência nominal a pessoa viva; texto ficcional “ASTRA CORP” no traje | JPEG legível com extensão `.png` |
| `drawable/img_sci_curie.png` | “Marie Curie-ous”; nome evoca cientista histórica, retrato não reproduz sua aparência conhecida de modo claro | JPEG legível com extensão `.png` |
| `drawable/img_sci_ada.png` | “Ada Lovelace-space”; nome evoca cientista histórica, retrato futurista sem semelhança óbvia | JPEG legível com extensão `.png` |
| `drawable/img_sci_albert.png` | “Albert Ein-stone”; cabelo, bigode e contexto científico evocam Albert Einstein | JPEG legível com extensão `.png` |
| `drawable/img_sci_karl.png` | “Karl Mark-IV”; nome evoca Karl Marx, mas o retrato não mostra semelhança inequívoca | JPEG legível com extensão `.png` |
| `drawable/img_sci_nebula.png` | Arte cósmica do pesquisador global; sem pessoa retratada | JPEG legível com extensão `.png` |
| `drawable/img_sci_helio_forja.webp` | Retrato de pesquisador fictício | WebP legível |
| `drawable/img_sci_iris_volt.webp` | Retrato de pesquisadora fictícia | WebP legível |
| `drawable/img_sci_nara_tunel.webp` | Retrato de pesquisadora fictícia | WebP legível |
| `drawable/img_sci_otto_veio.webp` | Retrato de pesquisador fictício | WebP legível |
| `drawable/img_sci_ferrus_maxwell.webp` | Retrato de pesquisador fictício; “Maxwell” pode evocar sobrenome histórico, sem semelhança visual identificada | WebP legível |
| `drawable/img_sci_veda_calibre.webp` | Retrato de pesquisadora fictícia | WebP legível |
| `drawable/img_sci_kira_flux.webp` | Retrato de pesquisadora fictícia | WebP legível |
| `drawable/img_sci_pascal_nano.webp` | Retrato de pesquisador fictício; “Pascal” pode evocar sobrenome histórico, sem semelhança visual identificada | WebP legível |
| `drawable/img_sci_gaia_bloom.webp` | Retrato de pesquisadora fictícia | WebP legível |
| `drawable/img_sci_vita_sprint.webp` | Retrato de pesquisadora fictícia | WebP legível |
| `drawable/img_sci_lumen_poda.webp` | Retrato de pesquisadora fictícia | WebP legível |
| `drawable/img_sci_cora_circuit.webp` | Retrato de pesquisadora fictícia | WebP legível |
| `drawable/img_sci_nix_turbo.webp` | Retrato de pesquisador fictício | WebP legível |
| `drawable/img_sci_tessa_byte.webp` | Retrato de pesquisadora fictícia | WebP legível |
| `drawable/img_sci_nova_fissao.webp` | Retrato de pesquisadora fictícia; distinto do holograma N.O.V.A. | WebP legível |
| `drawable/img_sci_juno_impulso.webp` | Retrato de pesquisadora fictícia | WebP legível |
| `drawable/img_sci_sera_prisma.webp` | Retrato de pesquisadora fictícia | WebP legível |
| `drawable/img_sci_orion_doca.webp` | Retrato de pesquisador fictício | WebP legível |
| `drawable/img_sci_lyra_warp.webp` | Retrato de pesquisadora fictícia | WebP legível |
| `drawable/img_sci_vega_vetor.webp` | Retrato de pesquisadora fictícia | WebP legível |
| `drawable/img_scientist_card.png` | Fallback de retrato referenciado por `ResearcherVisuals.kt`; conteúdo visual não pôde ser examinado | **Inválido para decodificador de imagens** |
| `drawable/img_capsule_comum.png` | Cápsula comum, usada na loja | **Inválido para decodificador de imagens** |
| `drawable/img_capsule_epica.png` | Cápsula épica, usada na loja | **Inválido para decodificador de imagens** |
| `drawable/img_capsule_suprema.png` | Cápsula suprema, usada na loja | **Inválido para decodificador de imagens** |
| `drawable/img_combat_banner.png` | Banner de combate; nenhuma marca/personagem conhecido identificado na inspeção | JPEG legível com extensão `.png` |
| `drawable/img_space_banner.png` | Banner espacial; nenhuma marca/personagem conhecido identificado na inspeção | JPEG legível com extensão `.png` |
| `drawable-nodpi/img_nova_hologram.png` | Holograma N.O.V.A.; **Codex/OpenAI documentado** em `docs/nova-pilot.md` | PNG legível |

As 26 imagens de pesquisadores são seis arquivos antigos `.png` e vinte `.webp`. Quatro outros `.png` (três cápsulas e fallback) começam com bytes de substituição UTF-8 (`EF BF BD`) em vez de uma assinatura PNG e não abrem no Pillow; esse é um defeito **confirmado dos arquivos**, mas seu efeito no APK não foi testado nesta etapa por restrição expressa contra build. Oito `.png` legíveis contêm dados JPEG: seis retratos antigos e dois banners. A extensão incorreta merece correção técnica futura, sem presumir falha de exibição no Android.

## Termos oficiais relevantes, sem conclusão de exclusividade

- Os [Termos adicionais da API Gemini / Google AI Studio](https://ai.google.dev/gemini-api/terms) dizem que o Google não reivindica propriedade do conteúdo original gerado, pode gerar conteúdo igual ou semelhante a outros e atribui ao usuário responsabilidade pelo uso conforme a lei. Os [Termos das APIs Google](https://developers.google.com/terms/) exigem respeito a direitos de terceiros; conteúdo acessível por API pode ter direitos próprios. A [política de uso proibido de IA generativa](https://policies.google.com/terms/generative-ai/use-policy) também proíbe violações de propriedade intelectual e uso enganoso de identidade. Verificar quais versões vigoravam quando cada imagem foi gerada.
- Nos [Termos de Uso da OpenAI](https://openai.com/policies/terms-of-use/), entre usuário e OpenAI e na medida permitida pela lei, o usuário detém o output; deve ter direitos sobre o input e responder pelo conteúdo. O output não é necessariamente único e a cessão não abrange o output de terceiros. As [políticas de uso](https://openai.com/policies/usage-policies/) não permitem tentar infringir direitos alheios. Confirmar qual produto/contrato específico foi utilizado em cada geração Codex/OpenAI.
- Esses textos tratam da relação contratual com os provedores; **não provam originalidade, exclusividade autoral ou ausência de colisão com obras, marcas e imagem de pessoas**. A [Lei brasileira 9.610/1998, art. 11](https://planalto.gov.br/ccivil_03/leis/l9610.htm) define autor como pessoa física criadora; a proteção de cada resultado e de eventuais contribuições humanas exige análise jurídica individual.

## Achados e limites da inspeção visual

**Risco identificado:** cinco nomes do catálogo são trocadilhos explícitos com pessoas reais (“Elon Dusk-V”, “Marie Curie-ous”, “Ada Lovelace-space”, “Albert Ein-stone” e “Karl Mark-IV”). A arte de Albert reforça a associação pela aparência. Elon é uma pessoa viva; a combinação do nome paródico com a carta comercial merece revisão de direito de imagem/personalidade e de eventual impressão de endosso. Para Marie, Ada e Karl, os retratos inspecionados não demonstraram semelhança fisionômica inequívoca, mas os nomes continuam referenciando figuras históricas. Isso é identificação de risco, não conclusão de infração.

**Não identificado na inspeção dos 29 arquivos legíveis:** cópia visual evidente de personagem ficcional protegido ou logotipo de terceiro reconhecível. A imagem de Elon contém letreiros estilizados, inclusive “ASTRA CORP”; não houve pesquisa de anterioridade de marca nem comparação sistemática por imagem. Os quatro arquivos ilegíveis não puderam ser avaliados visualmente. Uma observação humana de baixa escala não substitui busca reversa, consulta de marcas no [INPI](https://www.gov.br/inpi/pt-br/servicos/marcas) ou parecer jurídico; a ausência de achado não é garantia de originalidade.

## Pendências antes de distribuir o beta

1. Recuperar registros de geração quando disponíveis (prompt, ferramenta/modelo, data, conta autorizada, arquivo-fonte e termos da época) e associá-los aos arquivos. Se inexistentes, manter a declaração do proprietário e a atribuição individual incerta, sem inventar prova.
2. Obter avaliação jurídica humana dos cinco nomes associados a pessoas reais, especialmente Elon e Albert, e do uso promocional dessas cartas. Examinar também os letreiros/nomes incorporados às imagens e marcas do produto no território de distribuição.
3. Em etapa técnica **separada e autorizada**, diagnosticar os quatro arquivos inválidos e testar o APK; não corrigir nem substituir artes nesta etapa documental.
4. Registrar versão dos termos efetivamente aceita nas ferramentas usadas e avaliar proteção autoral/uso comercial sob a lei aplicável. Os termos atuais não regularizam automaticamente uma eventual violação de direitos de terceiros.
