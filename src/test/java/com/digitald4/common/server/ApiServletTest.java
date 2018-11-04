package com.digitald4.common.server;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import com.digitald4.common.server.service.JSONService;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

public class ApiServletTest {
	@Mock private JSONService teamService = mock(JSONService.class);
	@Mock private JSONService playerService = mock(JSONService.class);

	private ApiServiceServlet apiServlet;

	private static final JSONObject lakers = new JSONObject()
			.put("id", 1)
			.put("name", "Lakers");
	private static final JSONObject clippers = new JSONObject()
			.put("id", 2)
			.put("name", "Clippers");
	private static final JSONObject cavs = new JSONObject()
			.put("id", 3)
			.put("name", "Cavs");
	private static final JSONObject warriors = new JSONObject()
			.put("id", 4)
			.put("name", "Warriors");
	private static final JSONObject bucks = new JSONObject()
			.put("id", 5)
			.put("name", "Bucks");
	private static final JSONObject knicks = new JSONObject()
			.put("id", 6)
			.put("name", "Knicks");
	private static final JSONObject teams = new JSONObject().put("result", new JSONArray()
			.put(lakers)
			.put(clippers)
			.put(cavs)
			.put(warriors)
			.put(bucks)
			.put(knicks));

	private static final JSONObject eastTeams = new JSONObject().put("result", new JSONArray()
			.put(cavs)
			.put(bucks)
			.put(knicks));
	private static final JSONObject westTeams = new JSONObject().put("result", new JSONArray()
			.put(lakers)
			.put(clippers)
			.put(warriors));

	private static final JSONObject kobe = new JSONObject()
			.put("id", "24")
			.put("firstName", "Kobe")
			.put("lastName", "Bryant");

	private static final JSONObject lebron = new JSONObject()
			.put("id", "23")
			.put("firstName", "LeBron")
			.put("lastName", "James");

	private static final JSONObject lakerPlayers = new JSONObject().put("result", new JSONArray()
			.put(new JSONObject().put("name", "D. Russ"))
			.put(new JSONObject().put("name", "Swaggy P."))
			.put(new JSONObject().put("name", "B. Ingram"))
			.put(new JSONObject().put("name", "Loul Dang"))
			.put(new JSONObject().put("name", "Mozgov")));

	private static final JSONObject lakerVote = new JSONObject().put("vote", 1000);
	private static final JSONObject clipperVote = new JSONObject().put("vote", -3);

	@Before
	public void setup() throws Exception {
		apiServlet = new ApiServiceServlet()
				.addService("team", teamService)
				.addService("player", playerService);

		when(teamService.performAction(eq("get"), any(JSONObject.class))).then(invocation -> {
			switch (invocation.getArgumentAt(1, JSONObject.class).getInt("id")) {
				case 1:
					return lakers;
				case 2:
					return clippers;
				case 3:
					return cavs;
				case 4:
					return warriors;
				default:
					return null;
			}
		});

		when(teamService.performAction(eq("list"), any(JSONObject.class))).then(invocation -> {
			JSONObject request = invocation.getArgumentAt(1, JSONObject.class);
			if (request.has("conf")) {
				return (request.getString("conf").equals("east")) ? eastTeams : westTeams;
			}
			return teams;
		});

		when(teamService.performAction(eq("update"), any(JSONObject.class))).then(invocation -> {
			JSONObject updateRequest = invocation.getArgumentAt(1, JSONObject.class);
			JSONObject update = updateRequest.getJSONArray("update").getJSONObject(0);
			switch (updateRequest.getInt("id")) {
				case 1:
					return lakers;
				case 2:
					return clippers.put(update.getString("property"), update.getString("value"));
				case 3:
					return cavs;
				case 4:
					return warriors;
				default:
					return null;
			}
		});

		Answer<JSONObject> voteAnswer = invocation -> {
			switch (invocation.getArgumentAt(1, JSONObject.class).getInt("id")) {
				case 1:
					return lakerVote.put("vote", lakerVote.getInt("vote") + 1);
				case 2:
					return clipperVote.put("vote", clipperVote.getInt("vote") - 1);
				default:
					return null;
			}
		};

		when(teamService.performAction(eq("upvote"), any(JSONObject.class))).then(voteAnswer);
		when(teamService.performAction(eq("downvote"), any(JSONObject.class))).then(voteAnswer);
		when(playerService.performAction(eq("get"), any(JSONObject.class))).then(invocation -> {
			JSONObject request = invocation.getArgumentAt(1, JSONObject.class);
			int teamId = request.getInt("team_id");
			int playerId = request.getInt("id");
			if (teamId == lakers.getInt("id")) {
				if (playerId == 24) {
					return kobe;
				}
			} else if (teamId == cavs.getInt("id")) {
				if (playerId == 23) {
					return lebron;
				}
			}
			return null;
		});

		when(playerService.performAction(eq("list"), any(JSONObject.class))).then(invocation -> {
			JSONObject request = invocation.getArgumentAt(1, JSONObject.class);
			int teamId = request.getInt("team_id");
			if (teamId == lakers.getInt("id")) {
				return lakerPlayers;
			}
			return null;
		});
	}

	@Test @Ignore
	public void testCreate() {
	}

	@Test
	public void testGet() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRequestURL())
				.thenReturn(new StringBuffer("http://www.basketball.digitald4.com/api/teams/1"))
				.thenReturn(new StringBuffer("http://www.basketball.digitald4.com/api/teams/2"))
				.thenReturn(new StringBuffer("http://www.basketball.digitald4.com/api/teams/1/players/24"))
				.thenReturn(new StringBuffer("http://www.basketball.digitald4.com/api/teams/3/players/23"));
		when(request.getParameterMap()).thenReturn(new HashMap<>());
		HttpServletResponse response = mock(HttpServletResponse.class);
		PrintWriter writer = mock(PrintWriter.class);
		when(response.getWriter()).thenReturn(writer);
		apiServlet.doGet(request, response);
		verify(writer).println(lakers);
		apiServlet.doGet(request, response);
		verify(writer).println(clippers);
		apiServlet.doGet(request, response);
		verify(writer).println(kobe);
		apiServlet.doGet(request, response);
		verify(writer).println(lebron);
	}

	@Test
	public void testList() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRequestURL())
				.thenReturn(new StringBuffer("http://www.basketball.digitald4.com/api/teams"))
				.thenReturn(new StringBuffer("http://www.basketball.digitald4.com/api/conf/east/teams"))
				.thenReturn(new StringBuffer("http://www.basketball.digitald4.com/api/conf/west/teams"))
				.thenReturn(new StringBuffer("http://www.basketball.digitald4.com/api/teams/1/players"));
		when(request.getParameterMap()).thenReturn(new HashMap<>());
		HttpServletResponse response = mock(HttpServletResponse.class);
		PrintWriter writer = mock(PrintWriter.class);
		when(response.getWriter()).thenReturn(writer);
		apiServlet.doGet(request, response);
		verify(writer).println(teams);

		apiServlet.doGet(request, response);
		verify(writer).println(eastTeams);

		apiServlet.doGet(request, response);
		verify(writer).println(westTeams);

		apiServlet.doGet(request, response);
		verify(writer).println(lakerPlayers);
	}

	@Test
	public void testUpdate() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRequestURL())
				.thenReturn(new StringBuffer("http://www.basketball.digitald4.com/api/teams/2"));
		Map<String, String[]> payload = new HashMap<>();
		String json = "{update: [{property: \"name\", value: \"Flippers\"}]}";
		payload.put("json", new String[]{json});
		when(request.getParameterMap()).thenReturn(payload);
		when(request.getParameter("json")).thenReturn(json);
		HttpServletResponse response = mock(HttpServletResponse.class);
		PrintWriter writer = mock(PrintWriter.class);
		when(response.getWriter()).thenReturn(writer);
		apiServlet.doPut(request, response);
		verify(writer).println(clippers);
		assertEquals("Flippers", clippers.getString("name"));
	}

	@Test
	public void testCustomAction() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRequestURL())
				.thenReturn(new StringBuffer("http://www.basketball.digitald4.com/api/teams/1:upvote"))
				.thenReturn(new StringBuffer("http://www.basketball.digitald4.com/api/teams/2:downvote"));
		when(request.getParameterMap()).thenReturn(new HashMap<>());
		HttpServletResponse response = mock(HttpServletResponse.class);
		PrintWriter writer = mock(PrintWriter.class);
		when(response.getWriter()).thenReturn(writer);
		apiServlet.doGet(request, response);
		verify(writer).println(lakerVote);

		apiServlet.doGet(request, response);
		apiServlet.doGet(request, response);
		verify(writer, times(2)).println(clipperVote);
	}
}
