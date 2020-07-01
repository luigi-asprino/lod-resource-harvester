package it.cnr.istc.stlab.pss.harvester;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.cnr.istc.stlab.lgu.commons.files.FileUtils;

public class TaskBuilder {

	private static Logger logger = LoggerFactory.getLogger(TaskBuilder.class);

	public static List<DownloadTask> getTasks() {

		String json = FileUtils.readFile(HarvesterConfiguration.getPSSConfiguration().getTaskFile());
		JSONObject obj = new JSONObject(json);
		JSONArray arr = obj.getJSONArray("tasks");
		List<DownloadTask> result = new ArrayList<>();

		for (int i = 0; i < arr.length(); i++) {

			JSONObject taskJSON = arr.getJSONObject(i);

			String filter = null;
			if (taskJSON.has("filter") && !taskJSON.isNull("filter")) {
				filter = taskJSON.getString("filter");
			}

			String sparqlResourceSelector = null;
			if (taskJSON.has("sparqlResourceSelector") && !taskJSON.isNull("sparqlResourceSelector")) {
				sparqlResourceSelector = taskJSON.getString("sparqlResourceSelector");
			}

			String klass = null;
			if (taskJSON.has("class") && !taskJSON.isNull("class")) {
				klass = taskJSON.getString("class");
			}

			String graph = null;
			if (taskJSON.has("graph") && !taskJSON.isNull("graph")) {
				graph = taskJSON.getString("graph");
			}

			int limit = -1;
			if (taskJSON.has("limit") && !taskJSON.isNull("limit")) {
				limit = taskJSON.getInt("limit");
			}

			int pagination = -1;
			if (taskJSON.has("pagination") && !taskJSON.isNull("pagination")) {
				pagination = taskJSON.getInt("pagination");
			}

			String[] adpr = null;
			if (taskJSON.has("additionalPredicates") && !taskJSON.isNull("additionalPredicates")) {
				JSONArray a = taskJSON.getJSONArray("additionalPredicates");
				adpr = new String[a.length()];
				for (int ia = 0; ia < a.length(); ia++) {
					adpr[ia] = a.getString(ia);
				}
			}

			List<String> resourcesToGet = new ArrayList<>();
			if (taskJSON.has("resourcesToGet") && !taskJSON.isNull("resourcesToGet")) {
				JSONArray a = taskJSON.getJSONArray("resourcesToGet");
				for (int ia = 0; ia < a.length(); ia++) {
					logger.trace("adding spot resource " + a.getString(ia));
					resourcesToGet.add(a.getString(ia));
				}
			}

			String[] pte = null;
			if (taskJSON.has("patternToResourceToExpand") && !taskJSON.isNull("patternToResourceToExpand")) {
				JSONArray a = taskJSON.getJSONArray("patternToResourceToExpand");
				pte = new String[a.length()];
				for (int ia = 0; ia < a.length(); ia++) {
					pte[ia] = a.getString(ia);
				}
			}

			String[] qta = null;
			if (taskJSON.has("triplesToAdd") && !taskJSON.isNull("triplesToAdd")) {
				JSONArray a = taskJSON.getJSONArray("triplesToAdd");
				qta = new String[a.length()];
				for (int ia = 0; ia < a.length(); ia++) {
					qta[ia] = a.getString(ia);
				}
			}

			boolean useOnlyConstruct = false;
			if (taskJSON.has("useOnlyConstruct") && !taskJSON.isNull("triplesToAdd")) {
				useOnlyConstruct = taskJSON.getBoolean("useOnlyConstruct");
			}

			List<LODSecondarySource> secondarySources = new ArrayList<>();

			if (taskJSON.has("otherSources") && !taskJSON.isNull("otherSources")) {
				JSONArray a = taskJSON.getJSONArray("otherSources");
				for (int ia = 0; ia < a.length(); ia++) {
					JSONObject jsonSecondarySource = a.getJSONObject(ia);
					JSONArray queriesJSON = jsonSecondarySource.getJSONArray("queries");
					String[] queries = new String[queriesJSON.length()];
					for (int ib = 0; ib < queriesJSON.length(); ib++) {
						queries[ib] = queriesJSON.getString(ib);
					}

					LODSecondarySource ss = new LODSecondarySource(jsonSecondarySource.getString("endpoint"), queries);
					if (jsonSecondarySource.has("pathForIdentifyingURIInExternalSource")
							&& !jsonSecondarySource.isNull("pathForIdentifyingURIInExternalSource")) {
						ss.setPatternToIdentifyURIPointedToExternalSource(
								jsonSecondarySource.getString("pathForIdentifyingURIInExternalSource"));
					}
					secondarySources.add(ss);
				}
			}

			if (taskJSON.has("remoteDestination") && !taskJSON.isNull("remoteDestination")) {

				JSONObject remoteDestionationJSON = taskJSON.getJSONObject("remoteDestination");

				String password = null;
				if (!remoteDestionationJSON.isNull("password")) {
					password = remoteDestionationJSON.getString("password");
				}

				// @f:off
				result.add(new DownloadTask(
						new LODPrimarySource(taskJSON.getString("endpoint"), graph, sparqlResourceSelector, klass, adpr,
								pte, qta, filter, useOnlyConstruct,pagination).setSecondarySources(secondarySources)
										.setResourcesToGet(resourcesToGet),
						new LocalDestination(taskJSON.getString("localDestination")),
						new RemoteDestination(remoteDestionationJSON.getString("host"),
								remoteDestionationJSON.getString("user"), password,
								remoteDestionationJSON.getString("directory")),
						limit));
				// @f:on
			} else {

				// @f:off
				result.add(new DownloadTask(
						new LODPrimarySource(taskJSON.getString("endpoint"), graph, sparqlResourceSelector, klass, adpr,
								pte, qta, filter, useOnlyConstruct,pagination).setResourcesToGet(resourcesToGet),
						new LocalDestination(taskJSON.getString("localDestination")), null, limit));
				// @f:off
			}
		}

		return result;
	}

}
