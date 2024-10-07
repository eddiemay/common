package com.digitald4.common.util;

import com.digitald4.common.model.DataFile;
import com.digitald4.common.model.Email;
import com.digitald4.common.model.Email.Recipient;
import com.digitald4.common.storage.Store;
import com.google.common.collect.ImmutableList;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;
import javax.mail.*;
import javax.mail.Message.RecipientType;
import javax.mail.internet.*;


public class Emailer {
	private final Session session;
	private final Store<DataFile, String> dataFileStore;
	
	public Emailer(String host, final String username, final String password, Store<DataFile, String> dataFileStore) {
		this.dataFileStore = dataFileStore;

		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", host);
		//props.put("mail.smtp.port", "587");
 
		session = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});
	}

	//multi to, multi cc, multi bcc
	public Email sendmail(Email email) throws MessagingException, UnsupportedEncodingException {
		// create a message
		MimeMessage msg = new MimeMessage(session);
		msg.setContentLanguage(new String[]{"en"});
		msg.setFrom(new InternetAddress(email.getFrom()));

		email.getRecipients().forEach(r -> {
      try {
        msg.addRecipient(
						switch (r.getType()) {
							case To -> RecipientType.TO;
							case Cc -> RecipientType.CC;
							case Bcc -> RecipientType.BCC;
						},
						new InternetAddress(r.getAddress(), r.getName()));
      } catch (MessagingException | UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    });

		msg.setSubject(email.getSubject());
		msg.setSentDate(new Date());

		Multipart mp = new MimeMultipart();

		MimeBodyPart htmlPart = new MimeBodyPart();
		htmlPart.setContent(email.getMessage().toString(), "text/html");
		mp.addBodyPart(htmlPart);

		if (email.getFileReferences() != null) {
			email.getFileReferences().forEach(fileReference -> {
				try {
					MimeBodyPart attachment = new MimeBodyPart();
					DataFile dataFile = dataFileStore.get(fileReference.getName());
					InputStream attachmentDataStream = new ByteArrayInputStream(dataFile.getData());
					attachment.setFileName(fileReference.getName());
					attachment.setContent(attachmentDataStream, "application/" + fileReference.getType());
					mp.addBodyPart(attachment);
				} catch (MessagingException e) {
					throw new RuntimeException(e);
				}
			});
		}

		msg.setContent(mp);
		Transport.send(msg);
		return email;
	}

	public static void main (String[] args) {
		// String from = "noreply@iisos.net";
		String from = "eddiemay@gmail.com";
		String to = "eddiemay1999@yahoo.com";
		String subject = "Link to Gentoo website";
		String message = "Test message <a href=\"http://www.gentoo.org\">Gentoo Link</a>";

		try {
			Email email = new Emailer("imap.gmail.com", "eddiemay@gmail.com", "wtbx ewxu otfv txvs", null).sendmail(
					new Email()
							.setFrom(from)
							.setRecipients(ImmutableList.of(Recipient.createTo(to, null)))
							.setSubject(subject)
							.setMessage(message));
			System.out.printf("Message sent:\n\tFrom: %s\n\tTo: %s\n\tSubject: %s\n\tBody: %s\n",
					email.getFrom(), email.getRecipients(), email.getSubject(), email.getMessage());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}
