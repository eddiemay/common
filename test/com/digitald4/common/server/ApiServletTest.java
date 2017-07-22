package com.digitald4.common.server;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import com.digitald4.common.proto.DD4UIProtos;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class ApiServletTest {
	@Mock private JSONService teamService = mock(JSONService.class);
	@Mock private JSONService playerService = mock(JSONService.class);

	private ApiServiceServlet apiServlet;

	private static JSONObject lakers;
	private static JSONObject clippers;
	private static JSONObject cavs;
	private static JSONObject warriors;
	private static JSONObject bucks;
	private static JSONObject knicks;
	private static JSONArray teams;

	private static JSONArray eastTeams;
	private static JSONArray westTeams;

	private static JSONObject kobe;
	private static JSONObject lebron;

	private static JSONArray lakerPlayers;

	private static JSONObject flippers;

	@BeforeClass
	public static void initTeams() throws Exception {
		lakers = new JSONObject()
				.put("id", 1)
				.put("name", "Lakers");
		clippers = new JSONObject()
				.put("id", 2)
				.put("name", "Clippers");
		cavs = new JSONObject()
				.put("id", 3)
				.put("name", "Cavs");
		warriors = new JSONObject()
				.put("id", 4)
				.put("name", "Warriors");
		bucks = new JSONObject()
				.put("id", 5)
				.put("name", "Bucks");
		knicks = new JSONObject()
				.put("id", 6)
				.put("name", "Knicks");
		teams = new JSONArray()
				.put(lakers)
				.put(clippers)
				.put(cavs)
				.put(warriors)
				.put(bucks)
				.put(knicks);

		eastTeams = new JSONArray()
				.put(lakers)
				.put(clippers)
				.put(warriors);
		westTeams = new JSONArray()
				.put(cavs)
				.put(bucks)
				.put(knicks);

		kobe = new JSONObject()
				.put("id", "24")
				.put("firstName", "Kobe")
				.put("lastName", "Bryant");

		lebron = new JSONObject()
				.put("id", "23")
				.put("firstName", "LeBron")
				.put("lastName", "James");

		lakerPlayers = new JSONArray()
				.put(new JSONObject().put("name", "D. Russ"))
				.put(new JSONObject().put("name", "Swaggy P."))
				.put(new JSONObject().put("name", "B. Ingram"))
				.put(new JSONObject().put("name", "Loul Dang"))
				.put(new JSONObject().put("name", "Mozgov"));

		flippers = new JSONObject()
				.put("id", 2)
				.put("name", "Flippers");
	}

	@Before
	public void setup() throws Exception {
		apiServlet = new ApiServiceServlet()
				.addService("team", teamService)
				.addService("player", playerService);
		when(teamService.performAction(eq("get"), any(JSONObject.class)))
				.then(new Answer<Object>() {
					@Override
					public Object answer(InvocationOnMock invocation) throws Throwable {
						switch (invocation.getArgumentAt(1, JSONObject.class).getInt("id")) {
							case 1: return lakers;
							case 2: return clippers;
							case 3: return cavs;
							case 4: return warriors;
							default: return null;
						}
					}
				});
		when(teamService.performAction(eq("list"), any(JSONObject.class)))
				.then(new Answer<Object>() {
					@Override
					public Object answer(InvocationOnMock invocation) throws Throwable {
						JSONObject request = invocation.getArgumentAt(1, JSONObject.class);
						if (request.has("conf")) {
							return (request.getString("conf").equals("east")) ? eastTeams : westTeams;
						}
						return teams;
					}
				});
		when(teamService.performAction(eq("update"), any(JSONObject.class)))
				.then(new Answer<Object>() {
					@Override
					public Object answer(InvocationOnMock invocation) throws Throwable {
						JSONObject updateRequest = invocation.getArgumentAt(1, JSONObject.class);
						JSONObject update = updateRequest.getJSONArray("update").getJSONObject(0);
						switch (updateRequest.getInt("id")) {
							case 1: return lakers;
							case 2: return clippers.put(update.getString("property"), update.getString("value"));
							case 3: return cavs;
							case 4: return warriors;
							default: return null;
						}
					}
				});
		Answer<Integer> voteAnswer = new Answer<Integer>() {
			@Override
			public Integer answer(InvocationOnMock invocation) throws Throwable {
				switch (invocation.getArgumentAt(1, JSONObject.class).getInt("id")) {
					case 1: return 1000;
					case 2: return -3;
					case 3: return 450;
					case 4: return 700;
					default: return null;
				}
			}
		};
		when(teamService.performAction(eq("upvote"), any(JSONObject.class))).then(voteAnswer);
		when(teamService.performAction(eq("downvote"), any(JSONObject.class))).then(voteAnswer);
		when(playerService.performAction(eq("get"), any(JSONObject.class)))
				.then(new Answer<Object>() {
					@Override
					public Object answer(InvocationOnMock invocation) throws Throwable {
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
					}
				});
		when(playerService.performAction(eq("list"), any(JSONObject.class)))
				.then(new Answer<Object>() {
					@Override
					public Object answer(InvocationOnMock invocation) throws Throwable {
						JSONObject request = invocation.getArgumentAt(1, JSONObject.class);
						int teamId = request.getInt("team_id");
						if (teamId == lakers.getInt("id")) {
							return lakerPlayers;
						}
						return null;
					}
				});
	}

	@Test
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
				.thenReturn(new StringBuffer("http://www.basketball.digitald4.com/api/teams/1/upvote"))
				.thenReturn(new StringBuffer("http://www.basketball.digitald4.com/api/teams/2/downvote"));
		when(request.getParameterMap()).thenReturn(new HashMap<>());
		HttpServletResponse response = mock(HttpServletResponse.class);
		PrintWriter writer = mock(PrintWriter.class);
		when(response.getWriter()).thenReturn(writer);
		apiServlet.doGet(request, response);
		verify(writer).println(new Integer(1000));

		apiServlet.doGet(request, response);
		apiServlet.doGet(request, response);
		verify(writer, times(2)).println(new Integer(-3));
	}
}
