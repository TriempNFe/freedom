package org.freedom.persist.entity;

// Generated 18/02/2014 15:53:28 by Hibernate Tools 4.0.0

import java.util.Date;

/**
 * Vditdetorc generated by hbm2java
 */
public class Vditdetorc implements java.io.Serializable {

	private VditdetorcId id;
	private Vditgruporc vditgruporc;
	private Vddetorc vddetorc;
	private String textoitdetorc;
	private char itensdetorc;
	private Date dtins;
	private Date hins;
	private String idusuins;
	private Date dtalt;
	private Date halt;
	private String idusualt;

	public Vditdetorc() {
	}

	public Vditdetorc(VditdetorcId id, Vditgruporc vditgruporc,
			Vddetorc vddetorc, String textoitdetorc, char itensdetorc,
			Date dtins, Date hins, String idusuins) {
		this.id = id;
		this.vditgruporc = vditgruporc;
		this.vddetorc = vddetorc;
		this.textoitdetorc = textoitdetorc;
		this.itensdetorc = itensdetorc;
		this.dtins = dtins;
		this.hins = hins;
		this.idusuins = idusuins;
	}

	public Vditdetorc(VditdetorcId id, Vditgruporc vditgruporc,
			Vddetorc vddetorc, String textoitdetorc, char itensdetorc,
			Date dtins, Date hins, String idusuins, Date dtalt, Date halt,
			String idusualt) {
		this.id = id;
		this.vditgruporc = vditgruporc;
		this.vddetorc = vddetorc;
		this.textoitdetorc = textoitdetorc;
		this.itensdetorc = itensdetorc;
		this.dtins = dtins;
		this.hins = hins;
		this.idusuins = idusuins;
		this.dtalt = dtalt;
		this.halt = halt;
		this.idusualt = idusualt;
	}

	public VditdetorcId getId() {
		return this.id;
	}

	public void setId(VditdetorcId id) {
		this.id = id;
	}

	public Vditgruporc getVditgruporc() {
		return this.vditgruporc;
	}

	public void setVditgruporc(Vditgruporc vditgruporc) {
		this.vditgruporc = vditgruporc;
	}

	public Vddetorc getVddetorc() {
		return this.vddetorc;
	}

	public void setVddetorc(Vddetorc vddetorc) {
		this.vddetorc = vddetorc;
	}

	public String getTextoitdetorc() {
		return this.textoitdetorc;
	}

	public void setTextoitdetorc(String textoitdetorc) {
		this.textoitdetorc = textoitdetorc;
	}

	public char getItensdetorc() {
		return this.itensdetorc;
	}

	public void setItensdetorc(char itensdetorc) {
		this.itensdetorc = itensdetorc;
	}

	public Date getDtins() {
		return this.dtins;
	}

	public void setDtins(Date dtins) {
		this.dtins = dtins;
	}

	public Date getHins() {
		return this.hins;
	}

	public void setHins(Date hins) {
		this.hins = hins;
	}

	public String getIdusuins() {
		return this.idusuins;
	}

	public void setIdusuins(String idusuins) {
		this.idusuins = idusuins;
	}

	public Date getDtalt() {
		return this.dtalt;
	}

	public void setDtalt(Date dtalt) {
		this.dtalt = dtalt;
	}

	public Date getHalt() {
		return this.halt;
	}

	public void setHalt(Date halt) {
		this.halt = halt;
	}

	public String getIdusualt() {
		return this.idusualt;
	}

	public void setIdusualt(String idusualt) {
		this.idusualt = idusualt;
	}

}
