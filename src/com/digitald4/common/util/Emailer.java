package com.digitald4.common.util;

import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;


public class Emailer {
	private final Session session;
	
	public Emailer(String host, final String username, final String password) {
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		//props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", host);
		//props.put("mail.smtp.port", "587");
 
		session = Session.getInstance(props,
		  new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});
	}

	//1 to, no cc, no bcc
	public void sendmail(String from, String to, String subject, String message)
			throws AddressException, MessagingException{
		sendmail(from, new String[]{to} , subject, message, new String[]{}, new String[]{});
	}
	//1 to, 1 cc, no bcc
	public void sendmail(String from, String to, String subject, String message, String cc)
			throws AddressException, MessagingException{
		sendmail(from, new String[]{to} , subject, message, new String[]{cc}, new String[]{});
	}
	//1 to, 1 cc, 1 bcc
	public void sendmail(String from, String to, String subject, String message, String cc, String bcc)
			throws AddressException, MessagingException{
		sendmail(from, new String[]{to} , subject, message, new String[]{cc}, new String[]{bcc});
	}
	//multi to, no cc, no bcc
	public void sendmail(String from, String[] to, String subject, String message)
			throws AddressException, MessagingException{
		sendmail(from, to, subject, message, new String[]{}, new String[]{});
	}
	//multi to, 1 cc, no bcc
	public void sendmail(String from, String[] to, String subject, String message, String cc)
			throws AddressException, MessagingException{
		sendmail(from, to, subject, message, new String[]{cc}, new String[]{});
	}
	//multi to, 1 cc, 1 bcc
	public void sendmail(String from, String[] to, String subject, String message, String cc, String bcc)
			throws AddressException, MessagingException{
		sendmail(from, to, subject, message, new String[]{cc}, new String[]{bcc});
	}
	//multi to, multi cc, no bcc
	public void sendmail(String from, String[] to, String subject, String message, String[] cc) 
			throws AddressException, MessagingException {
		sendmail(from, to, subject, message, cc, new String[]{});
	}
	//multi to, multi cc, 1 bcc
	public void sendmail(String from, String[] to, String subject, String message, String[] cc, String bcc)
			throws AddressException, MessagingException{
		sendmail(from, to, subject, message, cc, new String[]{bcc});
	}
	//multi to, multi cc, multi bcc
	public void sendmail(String from, String tos[], String subject, String message, String ccs[], String bccs[])
			throws AddressException, MessagingException {
		// create a message
		MimeMessage msg = new MimeMessage(session);
		msg.setContentLanguage(new String[]{"en"});
		msg.setFrom(new InternetAddress(from));
		for (String to : tos) {
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
		}
		for (String cc : ccs) {
			msg.addRecipient(Message.RecipientType.CC, new InternetAddress(cc));
		}
		for (String bcc : bccs) {
			msg.addRecipient(Message.RecipientType.BCC, new InternetAddress(bcc));
		}
		msg.setSubject(subject);
		msg.setSentDate(new Date());
		msg.setContent(message, "text/html");
		Transport.send(msg);
	}

	public static void main (String args[]) {
		String to[] = new String[]{"eddiemay@gmail.com"};

		String from = "noreply@iisos.net";
		String cc = "eddiemay1999@yahoo.com";
		String subject = "Link to Gentoo website";
		String message = "Test message <a href=\"http://www.gentoo.org\">Gentoo Link</a>";

		try {
			Emailer emailer = new Emailer("mail.iisos.net", "noreply@iisos.net", ",{DE,TQJ!2NJ");
			emailer.sendmail(from, to, subject, message, cc);
			System.out.println("Message sent");
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}
