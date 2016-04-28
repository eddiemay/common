package com.digitald4.common.model;
import com.digitald4.common.dao.TransHistDAO;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
@Entity
@Table(schema="common",name="trans_hist")
@NamedQueries({
	@NamedQuery(name = "findByID", query="SELECT o FROM TransHist o WHERE o.ID=?1"),//AUTO-GENERATED
	@NamedQuery(name = "findAll", query="SELECT o FROM TransHist o"),//AUTO-GENERATED
	@NamedQuery(name = "findAllActive", query="SELECT o FROM TransHist o"),//AUTO-GENERATED
	@NamedQuery(name = "findByUser", query="SELECT o FROM TransHist o WHERE o.USER_ID=?1"),//AUTO-GENERATED
})
@NamedNativeQueries({
	@NamedNativeQuery(name = "refresh", query="SELECT o.* FROM trans_hist o WHERE o.ID=?"),//AUTO-GENERATED
})
public class TransHist extends TransHistDAO {
	
	public TransHist(EntityManager entityManager) {
		super(entityManager);
	}
	
	public TransHist(EntityManager entityManager, Integer id) {
		super(entityManager, id);
	}
	
	public TransHist(EntityManager entityManager, TransHist orig) {
		super(entityManager, orig);
	}
}
