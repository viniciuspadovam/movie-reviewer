# Regras de negócio

Os nomes entre parênteses são os identificadores usados no código (sempre em inglês).

## Obra (`Title`)

1. Toda obra é criada a partir do TMDB. O par (`tmdbId`, `mediaType`) é único; importar de novo uma obra existente retorna a já cadastrada, sem duplicar.
2. `mediaType` é `MOVIE` ou `SERIES`.
3. O `slug` é gerado a partir do nome e do ano (`clube-da-luta-1999`). Em caso de colisão, recebe o sufixo `-2`, `-3`… O slug não muda depois de criado, para não quebrar links.
4. Gêneros vêm do TMDB e são compartilhados entre obras.
5. Excluir uma obra sem reviews é permitido. Com reviews, a API responde `409` a menos que a chamada peça cascata explicitamente (`?cascade=true`), o que a UI só faz após uma confirmação.

## Review (`Review`)

6. Toda review pertence a uma obra. Cada review representa uma sessão (1ª vez, revisão…). A ordem das sessões é por `watchedOn` crescente, desempatando por `createdAt`.
7. Campos: data em que assistiu (`watchedOn`, não pode estar no futuro), nota (`rating`), texto em Markdown (`content`, obrigatório para publicar), contém spoiler (`hasSpoilers`) e status (`DRAFT | PUBLISHED`).
8. **Nota:** de 0,5 a 5 estrelas em passos de 0,5, guardada como inteiro de 1 a 10 (estrelas × 2). Fora dessa faixa é erro de validação.
9. A página da obra mostra a nota da sessão publicada mais recente como "nota atual" e, em cada sessão, a diferença em relação à sessão publicada anterior.
10. **Publicação:** ao passar para `PUBLISHED` pela primeira vez, `publishedAt` é preenchido e não muda mais (voltar a rascunho e republicar mantém a data original). "Últimas reviews" é ordenado por `publishedAt` decrescente.
11. **Edição:** altera a própria review e atualiza `updatedAt`. A UI mostra "editada em …" quando a edição ocorre depois da publicação. Não há histórico de edições na v1.
12. **Exclusão** de review é definitiva e exige confirmação na UI.
13. Reviews são acessadas por `id` (`/review/:id`). Não há slug de review.

## Visibilidade

14. Visitantes só veem reviews `PUBLISHED`. Rascunhos só aparecem nos endpoints `/admin/**`.
15. Obras sem nenhuma review publicada não aparecem na busca nem nos endpoints públicos (retornam `404`).
16. Reviews com spoiler aparecem ocultas atrás de um botão "Mostrar spoiler".

## Busca

17. A busca textual cobre o nome da obra (original e traduzido) e o texto das reviews publicadas, com stemming em português.
18. Filtros combináveis: tipo, gênero, faixa de nota (sobre a nota atual), ano de lançamento. Ordenações: mais recentes (padrão), melhor nota, título.

## Atribuição

19. O rodapé exibe o logo e a frase de atribuição exigidos pelos termos do TMDB.
