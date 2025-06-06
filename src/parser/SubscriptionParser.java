package parser;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import subscription.SingleSubscription;
import subscription.Subscription;

/**
 * Esta clase implementa el parser para el archivo de suscripción en formato JSON.
 * Se encarga de leer y procesar el archivo para generar objetos de suscripción.
 * Leer más en: https://www.w3docs.com/snippets/java/how-to-parse-json-in-java.html
 */
public class SubscriptionParser {

	private FileReader subscriptionFile;

	/**
	 * Constructor de la clase SubscriptionParser.
	 *
	 * @param filePath Ruta del archivo JSON de suscripciones.
	 * @throws FileNotFoundException Si el archivo no se encuentra en la ruta especificada.
	 */
	public SubscriptionParser(String filePath) throws FileNotFoundException {
		String fullPath = FileSystems.getDefault().getPath(filePath).toString();
		subscriptionFile = new FileReader(fullPath);
	}

	/**
	 * Parsea el archivo JSON y lo convierte en un JSONArray.
	 *
	 * @return Un JSONArray que representa el contenido del archivo JSON.
	 */
	private JSONArray parseJson() {
		JSONTokener tokener = new JSONTokener(subscriptionFile);
		return new JSONArray(tokener);
	}

	/**
	 * Verifica si un objeto JSON contiene los campos necesarios para ser una suscripción válida.
	 *
	 * @param o El objeto JSON a validar.
	 * @return true si el objeto contiene todos los campos requeridos, false en caso contrario.
	 */
	private boolean isValidSingleSubscription(JSONObject o) {
		return (
			o.has("url") &&
			o.has("urlParams") &&
			o.has("urlType")
		);
	}

	/**
	 * Convierte un objeto JSON en una instancia de SingleSubscription.
	 *
	 * @param o El objeto JSON que representa una suscripción individual.
	 * @return Una instancia de SingleSubscription con los datos del objeto JSON.
	 */
	private SingleSubscription parseSingleSubscription(JSONObject o) {
		String url = o.getString("url");
		String urlType = o.getString("urlType");
		List<String> urlParams = new ArrayList<String>();
		JSONArray blob = o.getJSONArray("urlParams");
		for (int i = 0; i < blob.length(); i++) {
			urlParams.add(blob.getString(i));
		}
		return new SingleSubscription(url, urlParams, urlType);
	}

	/**
	 * Procesa el archivo JSON y genera un objeto Subscription que contiene todas las suscripciones.
	 *
	 * @return Un objeto Subscription con las suscripciones procesadas.
	 * @throws InvalidFormatException Si algún objeto JSON no contiene los campos requeridos.
	 */
	public Subscription getSubscriptions() throws InvalidFormatException {
		Subscription res = new Subscription();
		JSONArray list = parseJson();
		for (int i = 0; i < list.length(); i++) {
			JSONObject blob = list.getJSONObject(i);
			if (!isValidSingleSubscription(blob)) {
				throw new InvalidFormatException("missing fields");
			}
			res.addSingleSubscription(parseSingleSubscription(blob));
		}
		return res;
	}
}
