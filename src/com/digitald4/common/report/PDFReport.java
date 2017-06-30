package com.digitald4.common.report;

import com.digitald4.common.proto.DD4Protos.Company;
import com.digitald4.common.util.Provider;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;

public abstract class PDFReport {
	private Image logo;
	private final Provider<Company> companyProvider;
	public PDFReport(Provider<Company> companyProvider) {
		this.companyProvider = companyProvider;
	}
	
	public abstract String getTitle();
	
	public String getSubject() {
		return getTitle();
	}
	
	public String getAuthor() {
		return "Digital D4";
	}
	
	public Paragraph getFooter() {
		Paragraph paragraph = new Paragraph();
		paragraph.add(getFooterText());
		return paragraph;
	}
	
	public String getFooterText() {
		return "";
	}
	
	public PDFReport setLogo(Image logo) {
		this.logo = logo;
		return this;
	}
	
	public Image getLogo() {
		return logo;
	}
	
	public Rectangle getPageSize() {
		return PageSize.A4;
	}
	
	public ByteArrayOutputStream createPDF() throws Exception {
		Document document = new Document(getPageSize(), 25, 25, 25, 25);
		document.addAuthor(getAuthor());
		document.addSubject(getSubject());
		document.addTitle(getTitle());
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		PdfWriter.getInstance(document, buffer);
		document.open();
		// document.resetHeader();
		//document.setHeader(getHeader());
		// document.setFooter(getFooter());
		document.setPageSize(getPageSize());
		document.setMargins(25, 25, 25, 25);
		document.newPage();
		document.add(getReportTitle());
		document.add(getBody());
		//document.add(getFooter());
		document.close();
		return buffer;
	}
	
	public Company getCompany() {
		return companyProvider.get();
	}
	
	public Paragraph getReportTitle() {
		Paragraph title = new Paragraph();
		title.setAlignment(Element.ALIGN_CENTER);
		Company company = getCompany();
		if (company != null && company.getName() != null && company.getName().length() > 0) {
			title.add(new Chunk(company.getName() + "\n", FontFactory.getFont(FontFactory.HELVETICA, 20, Font.BOLD)));
		}
		title.add(new Chunk(getTitle() + "\n", FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD)));
		return title;
	}
	
	/*public Header getHeader() {
		String DATE_FORMAT = "MM/dd/yyyy";
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(DATE_FORMAT);
		sdf.setTimeZone(TimeZone.getDefault());

		String website = "";

		Paragraph companyPara = new Paragraph("");
		
		Image logo = getLogo();
		if (logo != null) {
			logo.setAlignment(Image.LEFT);
			//logo.setAbsolutePosition(10, 10);
			logo.setAlignment(Image.LEFT | Image.UNDERLYING);
			logo.scaleAbsolute(100, 100);
			companyPara.add(logo);
		}

		Company company = Company.get();
		if (company != null) {
			companyPara.setAlignment(Element.ALIGN_CENTER);

			if (company.getName() != null && company.getName().length() > 0)
				companyPara.add(new Chunk(""+company.getName(), FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD)));

			if (company.getSlogan() != null && company.getSlogan().length() > 0)
				companyPara.add(new Chunk("\n"+company.getSlogan(), FontFactory.getFont(FontFactory.HELVETICA, 10, Font.ITALIC)));

			if (company.getAddress() != null && company.getAddress().length() > 0)
				companyPara.add(new Chunk("\n"+company.getAddress(), FontFactory.getFont(FontFactory.HELVETICA, 10)));

			if (company.getPhone() != null && company.getPhone().length() > 0)
				companyPara.add(new Chunk("\n Office "+company.getPhone(), FontFactory.getFont(FontFactory.HELVETICA, 10)));

			if (company.getFax() != null && company.getFax().length() > 0)
				companyPara.add(new Chunk("\n Fax "+company.getFax(), FontFactory.getFont(FontFactory.HELVETICA, 10)));

			if (company.getWebsite() != null && company.getWebsite().length() > 0) {
				website = company.getWebsite();
				companyPara.add(new Chunk("\n"+website, FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD)));
			}
		}
		Header header = new Header(companyPara, true);
		header.setBorder(Rectangle.NO_BORDER);
		return header;
	}*/
	
	public abstract Paragraph getBody() throws DocumentException, Exception;
	
	/*public Header getFooter() {
		Paragraph paragraph = new Paragraph();
		Company company = Company.get();
		if (company != null) {
			paragraph.add(new Phrase(company.getReportFooter(), FontFactory.getFont(FontFactory.HELVETICA, 8)));
		}
		paragraph.add(new Phrase("Generated: " + DateTime.now(), FontFactory.getFont(FontFactory.HELVETICA, 8)));;
		Header footer =      ;
		footer.setBorder(Rectangle.NO_BORDER);
		return footer;
	}*/
}
