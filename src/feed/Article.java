package feed;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import namedEntity.NamedEntity;
import namedEntity.categories.Person.*;
import namedEntity.categories.Place.*;
import namedEntity.categories.*;
import namedEntity.heuristic.Heuristic;
import namedEntity.topics.*;
// import namedEntity.topics.Topic;

/*Esta clase modela el contenido de un articulo (ie, un item en el caso del rss feed) */

public class Article {

	private String title;
	private String text;
	private Date publicationDate;
	private String link;

	private  List<NamedEntity> namedEntityList = new ArrayList<NamedEntity>();

	public Article(String title, String text, Date publicationDate, String link) {
		super();
		this.title = title;
		this.text = text;
		this.publicationDate = publicationDate;
		this.link = link;
		this.namedEntityList = new ArrayList<NamedEntity>();
	}

	
	public List<NamedEntity> getNamedEntityList() {
		return this.namedEntityList;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Date getPublicationDate() {
		return publicationDate;
	}

	public void setPublicationDate(Date publicationDate) {
		this.publicationDate = publicationDate;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	@Override
	public String toString() {
		return (
			"Article [title=" +
			title +
			", text=" +
			text +
			", publicationDate=" +
			publicationDate +
			", link=" +
			link +
			"]"
		);
	}

	public NamedEntity getNamedEntity(String namedEntity) {
		for (NamedEntity n : this.namedEntityList) {
			if (n.getName().compareTo(namedEntity) == 0) {
				return n;
			}
		}
		return null;
	}

	
	public void computeNamedEntities(Heuristic h) {
        String fullText = this.getTitle() + " " + this.getText();

        String charsToRemove = ".,;:()'’!?\n";
        for (char c : charsToRemove.toCharArray()) {
            fullText = fullText.replace(String.valueOf(c), "");
        }

        String[] words = fullText.split(" ");
        int n = words.length;

        for (int i = 0; i < n; /* i se gestiona abajo */) {
            if (words[i].trim().isEmpty()) {
                i++;
                continue;
            }

            boolean entityProcessedInThisIteration = false;
            // Probar N-gramas desde el más largo (4) hasta el más corto (1)
            for (int ngramSize = Math.min(4, n - i); ngramSize >= 1; ngramSize--) {
                StringBuilder phraseBuilder = new StringBuilder();
                // 1. Construir el N-grama completo para el ngramSize actual
                for (int j = 0; j < ngramSize; j++) {
                    if (j > 0) {
                        phraseBuilder.append(" ");
                    }
                    phraseBuilder.append(words[i + j]);
                }
                String candidatePhrase = phraseBuilder.toString();

                // 2. Intentar procesar este N-grama completo
                // Primero, verificar si la forma canónica del N-grama está en categoryMap
                String canonicalName = h.getCanonical(candidatePhrase);
                Category categoryFromCanonicalMap = h.getCategory(canonicalName);

                if (!categoryFromCanonicalMap.isOther() && h.isEntity(candidatePhrase)) {
                    // CASO 1: El N-grama (vía su forma canónica) está definido en categoryMap.
                    NamedEntity ne = this.getNamedEntity(canonicalName);
                    if (ne == null) {
                        Topic topic = h.getTopic(canonicalName);
                        NamedEntity newEntity = new NamedEntity(canonicalName, 1, categoryFromCanonicalMap, topic);
                        if (newEntity != null) {
                            this.namedEntityList.add(newEntity);
                        }
                    } else {
                        ne.incFrequency();
                    }
                    i += ngramSize; // Avanzar i por el tamaño del N-grama encontrado
                    entityProcessedInThisIteration = true;
                    break; // Salir del bucle de ngramSize, N-grama más largo encontrado y procesado.
                }
                // Si el N-grama no estaba en categoryMap, y es una sola palabra (ngramSize == 1),
                // entonces aplicar la lógica de h.isEntity() (para QuickHeuristic)
                else if (ngramSize == 1 && h.isEntity(candidatePhrase)) { // candidatePhrase es una sola palabra aquí
                    // CASO 2: Es una palabra individual, no en categoryMap directamente, pero h.isEntity() es true.
                    // 'canonicalName' ya es h.getCanonical(candidatePhrase)
                    String actualCanonicalForSingleWord = canonicalName;
                     if (actualCanonicalForSingleWord == null || actualCanonicalForSingleWord.isEmpty()){
                            actualCanonicalForSingleWord = candidatePhrase; // Fallback si getCanonical dio nulo/vacío
                     }


                    NamedEntity ne = this.getNamedEntity(actualCanonicalForSingleWord);
                    if (ne == null) {
                        // Determinar categoría:
                        // 1. categoryFromCanonicalMap ya sabemos que es vacía.
                        // 2. Intentar categoría de la palabra original (candidatePhrase).
                        Category categoryForSingleWord = h.getCategory(candidatePhrase);
                        // if (categoryForSingleWord == null || categoryForSingleWord.isEmpty()) {
                        //     // 3. Si sigue sin categoría, es "Other".
                        //     categoryForSingleWord = "Other";
                        // }
                        Topic topic = h.getTopic(actualCanonicalForSingleWord);
                        NamedEntity newEntity = new NamedEntity(candidatePhrase,1,categoryForSingleWord,topic);
                        if (newEntity != null) {
                            this.namedEntityList.add(newEntity);
                        }
                    } else {
                        ne.incFrequency();
                    }
                    i += 1; // Avanzamos una palabra
                    entityProcessedInThisIteration = true;
                    break; // Salir del bucle de ngramSize, palabra individual procesada.
                }
            } // Fin del bucle ngramSize

            if (!entityProcessedInThisIteration) {
                // Si, después de probar todos los N-gramas (de 4 a 1) para words[i], no se procesó nada,
                // entonces simplemente avanzamos una palabra para la siguiente iteración.
                i++;
            }
        }
    }

	public void prettyPrint() {
		System.out.println(
			"**********************************************************************************************"
		);
		System.out.println("Title: " + this.getTitle());
		System.out.println("Publication Date: " + this.getPublicationDate());
		System.out.println("Link: " + this.getLink());
		System.out.println("Text: " + this.getText());
		System.out.println(
			"**********************************************************************************************"
		);
	}

	public  void showEntities() {
		for (NamedEntity entity: this.namedEntityList) {
			if(!entity.getCategory().equals("Other")){

				entity.prettyPrint();
			}
		}
	}

	public static void main(String[] args) {
		Article a = new Article(
			"This Historically Black University Created Its Own Tech Intern Pipeline",
			"A new program at Bowie State connects computing students directly with companies, bypassing an often harsh Silicon Valley vetting process",
			new Date(),
			"https://www.nytimes.com/2023/04/05/technology/bowie-hbcu-tech-intern-pipeline.html"
		);

		a.prettyPrint();
	}
}
