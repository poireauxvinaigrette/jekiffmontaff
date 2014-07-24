package controllers;

import java.io.UnsupportedEncodingException;
import java.util.Iterator;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import play.Logger;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.ws.WS;
import play.libs.ws.WSRequestHolder;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;

public class Badge extends Controller {

	public static Result index() {
		return ok(index.render("Your new application is ready."));
	}

	public static Result badge() {
		String html = "Impossible de se connecter";
		String matinD = "" , apremD = "";
		try {
			WSRequestHolder holder = WS.url("http://gtabadge.rh.recouv/webquartzacq/acq/badge.do").setContentType(
					"application/x-www-form-urlencoded");
			String postParam = "Connexion=y&DECALHOR=-120&TIME=1403092854390&Cpts=Display+counters&USERID=CER3100429&PASSWORD=xxxxxx";

			int nbTentative = 0;
			Promise<String> req;
			String result;

			boolean sucess;
			do {
				nbTentative++;

				Promise<String> prom = holder.post(postParam).map(new Function<WSResponse, String>() {
					public String apply(WSResponse response) throws UnsupportedEncodingException {
						byte[] json = response.asByteArray();
						return new String(json);
					}
				});

				result = prom.get(1500);
				Thread.sleep(1000);
				sucess = !result.contains("authform");
				Logger.debug("nbTentative : " + nbTentative + " Connection OK ? : " + sucess);
			} while (!sucess && nbTentative < 10);

			if (sucess) {

				DateTime haam = null, hdam = null, hapm = null, hdpm = null;

				org.jsoup.nodes.Document parse = Jsoup.parse(result);

				Logger.debug("Parsing :" + result);

				Elements select = parse.select("table[class=acqArray]");

				Elements prems = parse.select("td[class=acqpair titi]"); 
				Iterator iterator = prems.iterator();
				Element heure = null;

				if (iterator.hasNext()) {
					heure = (Element) iterator.next();
					haam = DateTime.parse(heure.text(), DateTimeFormat.forPattern("HH.mm"));
					Logger.debug("arrivée am : " + haam.toString("HH.mm"));
				}
				if (iterator.hasNext()) {
					heure = (Element) iterator.next();
					hapm = DateTime.parse(heure.text(), DateTimeFormat.forPattern("HH.mm"));
					Logger.debug("arrivée pm : " + hapm.toString("HH.mm"));
				}

				Elements select3 = parse.select("td[class=titi acqimp]");
				if (select3.first() != null) {
					hdam = DateTime.parse(select3.first().text(), DateTimeFormat.forPattern("HH.mm"));
					Logger.debug("départ am : " + hdam.toString("HH.mm"));
				}
				if (select3.last() != null) {
					hdpm = DateTime.parse(select3.last().text(), DateTimeFormat.forPattern("HH.mm"));
					Logger.debug("départ pm : " + hdpm.toString("HH.mm"));
				}

				Element last = select.last();

				Elements select5 = last.select("tr").select("td");// td:eq(2)");
				
				
				
				Duration matin = new Duration(haam, hapm);
				Duration aprem = new Duration(hdam, hdpm);
				
				matinD = matin.toString();//"H"+matin.getStandardHours()+"M"+matin.getStandardMinutes();
				apremD = "H"+aprem.getStandardHours()+"M"+aprem.getStandardMinutes();
			
				html = select5.text();
			}
		} catch (Exception e) {
			e.printStackTrace();

		}

		// return ok(index.render("Your new application is ready."));
		// return ok(reponse.toString());
		// ok(select5.html());
		return ok(views.html.badge.render(html,matinD, apremD));

	}
}
