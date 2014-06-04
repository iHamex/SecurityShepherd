package servlets.module.lesson;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Encoder;

import utils.ExposedServer;
import utils.FindXSS;
import utils.Hash;
import utils.ShepherdLogManager;
import utils.Validate;
import dbProcs.Getter;
/**
 * Unvalidated Redirects and Forwards Lesson
 * <br/><br/>
 * This file is part of the Security Shepherd Project.
 * 
 * The Security Shepherd project is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.<br/>
 * 
 * The Security Shepherd project is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.<br/>
 * 
 * You should have received a copy of the GNU General Public License
 * along with the Security Shepherd project.  If not, see <http://www.gnu.org/licenses/>. 
 * @author Mark Denihan
 *
 */
public class UnvalidatedForwardsLesson extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static org.apache.log4j.Logger log = Logger.getLogger(UnvalidatedForwardsLesson.class);
	private static String levelName = "Unvalidated Redirects and Forwards Lesson";
	private static String levelHash = "f15f2766c971e16e68aa26043e6016a0a7f6879283c873d9476a8e7e94ea736f";
	
	/**
	 * User submission is parsed for a valid URL. This is then used to construct a URL object. This URL object is then checked to ensure a valid attack
	 * @param tempId User's session stored temporary id
	 * @param messageForAdmin Users lesson submission
	 */
	public void doPost (HttpServletRequest request, HttpServletResponse response) 
	throws ServletException, IOException
	{
		//Setting IpAddress To Log and taking header for original IP if forwarded from proxy
		ShepherdLogManager.setRequestIp(request.getRemoteAddr(), request.getHeader("X-Forwarded-For"));
		log.debug(levelName +" Servlet Accessed");
		PrintWriter out = response.getWriter();  
		out.print(getServletInfo());
		Encoder encoder = ESAPI.encoder();
		try
		{
			HttpSession ses = request.getSession(true);
			if(Validate.validateSession(ses))
			{
				log.debug("Current User: " + ses.getAttribute("userName").toString());
				Cookie tokenCookie = Validate.getToken(request.getCookies());
				Object tokenParmeter = request.getParameter("csrfToken");
				if(Validate.validateTokens(tokenCookie, tokenParmeter))
				{
					String tempId = (String) ses.getAttribute("tempId");
					log.debug("tempId = " + tempId);
					String userName = (String) ses.getAttribute("userName");
					String messageForAdmin = request.getParameter("messageForAdmin").toLowerCase();
					log.debug("User Submitted - " + messageForAdmin);
					String htmlOutput = new String();
					boolean validUrl = true;
					boolean validSolution = false;
					boolean validAttack = false;
					try
					{
						URL csrfUrl = new URL(messageForAdmin);
						log.debug("Url Host: " + csrfUrl.getHost());
						log.debug("Url Port: " + csrfUrl.getPort());
						log.debug("Url Path: " + csrfUrl.getPath());
						log.debug("Url Query: " + csrfUrl.getQuery());
						validSolution = csrfUrl.getHost().equals(ExposedServer.getSecureHost().toLowerCase());
						if(!validSolution)
							log.debug("Invalid Solutoin: Bad Host");
						validSolution = new Integer(csrfUrl.getPort()).toString().equals(ExposedServer.getSecurePort().toLowerCase()) && validSolution;
						if(!validSolution)
							log.debug("Invalid Solutoin: Bad Port or Above");
						validSolution = csrfUrl.getPath().toLowerCase().equalsIgnoreCase("/user/redirect") && validSolution;
						if(!validSolution)
							log.debug("Invalid Solutoin: Bad Path or Above");
						validSolution = csrfUrl.getQuery().toLowerCase().startsWith(("to=").toLowerCase()) && validSolution;
						if(!validSolution)
							log.debug("Invalid Solutoin: Bad Query or Above");
						if(validSolution)
						{
							log.debug("Redirect URL Correct: Now checking the Redirected URL for valid CSRF Attack");
							int csrfStart = 0;
							int csrfEnd = 0;
							csrfStart = csrfUrl.getQuery().indexOf("to=") + 3;
							csrfEnd = csrfUrl.getQuery().indexOf("&");
							if(csrfEnd == -1)
							{
								csrfEnd = csrfUrl.getQuery().length();
							}
							String csrfAttack = csrfUrl.getQuery().substring(csrfStart, csrfEnd);
							log.debug("csrfAttack Found to be: " + csrfAttack);
							validAttack = FindXSS.findCsrfAttackUrl(csrfAttack, "/root/grantComplete/unvalidatedredirectlesson", "userId", tempId);
						}
					}
					catch(MalformedURLException e)
					{
						log.error("Invalid URL: " + e.toString());
						validUrl = false;
						validSolution = false;
						validAttack = false;
						messageForAdmin = "";
						htmlOutput="Invalid Url";
					}				
					
					if(validSolution && validAttack)
					{
						htmlOutput = "<h2 class='title'>Well Done</h2>" +
								"<p>The administrator recieved your messege, clicked the link, and submitted the GET request automaticaly through the invalidated redirect<br />" +
								"The result key for this lesson is <a>" +
								encoder.encodeForHTML(
										Hash.generateUserSolution(
												Getter.getModuleResultFromHash(getServletContext().getRealPath(""), levelHash
												), (String)ses.getAttribute("userName")
										)
								) +
								"</a>";
					}
					if(validUrl)
					{
						log.debug("Adding message to Html: " + messageForAdmin);
						htmlOutput += "<h2 class='title'>Message Sent</h2>" +
							"<p><table><tr><td>Sent To: </td><td>administrator@SecurityShepherd.com</td></tr>" +
							"<tr><td>Message: </td><td><a href='" +
							encoder.encodeForHTMLAttribute(messageForAdmin) +
							"'>" + encoder.encodeForHTML("Link from " + userName) +
							"</a></td></tr></table></p>";
					}
					log.debug("outputing HTML");
					out.write(htmlOutput);
				}
			}
		}
		catch(Exception e)
		{
			out.write("An Error Occured! You must be getting funky!");
			log.fatal(levelName + " - " + e.toString());
		}
		log.debug("End of " + levelName + " Servlet");
	}
}