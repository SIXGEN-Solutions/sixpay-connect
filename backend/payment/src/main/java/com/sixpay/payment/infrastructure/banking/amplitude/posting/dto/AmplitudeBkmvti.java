package com.sixpay.payment.infrastructure.banking.amplitude.posting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Full field inventory from the supplied legacy BKMVTI example.
 * Bank-specific data holder, NOT a SIXPAY persistence entity and NOT sent at T0.
 * Unknown fields and bank-owned dates/event numbers remain null.
 * Future persistence mapping requires an approved database migration.
 */
public class AmplitudeBkmvti {
    private String id;
    private String age;
    private String dev;
    private String cha;
    private String ncp;
    private String suf;
    private String ope;
    private String mvti;
    private String rgp;
    private String uti;
    private String eve;
    private String clc;
    private LocalDate dco;
    private String ser;
    private LocalDate dva;
    private BigDecimal mon;
    private String sen;
    private String lib;
    private String exo;
    private String pie;
    private String rlet;
    private String des1;
    private String des2;
    private String des3;
    private String des4;
    private String des5;
    private String utf;
    private String uta;
    private BigDecimal tau;
    private LocalDate din;
    private String tpr;
    private BigDecimal npr;
    private String ncc;
    private String suc;
    private String esi;
    private String imp;
    private String cta;
    private String mar;
    private LocalDate dech;
    private String agsa;
    private String agde;
    private String devc;
    private BigDecimal mctv;
    private String pieo;
    private String iden;
    private String lang;
    private String libnls;
    private String modu;
    private String refdos;
    private String refana;
    private String label;
    private String nat;
    private String eta;
    private String destana;
    private String fusion;

    public AmplitudeBkmvti() { }

    public String getId() { return id; }
    public void setId(String value) { this.id = value; }
    public String getAge() { return age; }
    public void setAge(String value) { this.age = value; }
    public String getDev() { return dev; }
    public void setDev(String value) { this.dev = value; }
    public String getCha() { return cha; }
    public void setCha(String value) { this.cha = value; }
    public String getNcp() { return ncp; }
    public void setNcp(String value) { this.ncp = value; }
    public String getSuf() { return suf; }
    public void setSuf(String value) { this.suf = value; }
    public String getOpe() { return ope; }
    public void setOpe(String value) { this.ope = value; }
    public String getMvti() { return mvti; }
    public void setMvti(String value) { this.mvti = value; }
    public String getRgp() { return rgp; }
    public void setRgp(String value) { this.rgp = value; }
    public String getUti() { return uti; }
    public void setUti(String value) { this.uti = value; }
    public String getEve() { return eve; }
    public void setEve(String value) { this.eve = value; }
    public String getClc() { return clc; }
    public void setClc(String value) { this.clc = value; }
    public LocalDate getDco() { return dco; }
    public void setDco(LocalDate value) { this.dco = value; }
    public String getSer() { return ser; }
    public void setSer(String value) { this.ser = value; }
    public LocalDate getDva() { return dva; }
    public void setDva(LocalDate value) { this.dva = value; }
    public BigDecimal getMon() { return mon; }
    public void setMon(BigDecimal value) { this.mon = value; }
    public String getSen() { return sen; }
    public void setSen(String value) { this.sen = value; }
    public String getLib() { return lib; }
    public void setLib(String value) { this.lib = value; }
    public String getExo() { return exo; }
    public void setExo(String value) { this.exo = value; }
    public String getPie() { return pie; }
    public void setPie(String value) { this.pie = value; }
    public String getRlet() { return rlet; }
    public void setRlet(String value) { this.rlet = value; }
    public String getDes1() { return des1; }
    public void setDes1(String value) { this.des1 = value; }
    public String getDes2() { return des2; }
    public void setDes2(String value) { this.des2 = value; }
    public String getDes3() { return des3; }
    public void setDes3(String value) { this.des3 = value; }
    public String getDes4() { return des4; }
    public void setDes4(String value) { this.des4 = value; }
    public String getDes5() { return des5; }
    public void setDes5(String value) { this.des5 = value; }
    public String getUtf() { return utf; }
    public void setUtf(String value) { this.utf = value; }
    public String getUta() { return uta; }
    public void setUta(String value) { this.uta = value; }
    public BigDecimal getTau() { return tau; }
    public void setTau(BigDecimal value) { this.tau = value; }
    public LocalDate getDin() { return din; }
    public void setDin(LocalDate value) { this.din = value; }
    public String getTpr() { return tpr; }
    public void setTpr(String value) { this.tpr = value; }
    public BigDecimal getNpr() { return npr; }
    public void setNpr(BigDecimal value) { this.npr = value; }
    public String getNcc() { return ncc; }
    public void setNcc(String value) { this.ncc = value; }
    public String getSuc() { return suc; }
    public void setSuc(String value) { this.suc = value; }
    public String getEsi() { return esi; }
    public void setEsi(String value) { this.esi = value; }
    public String getImp() { return imp; }
    public void setImp(String value) { this.imp = value; }
    public String getCta() { return cta; }
    public void setCta(String value) { this.cta = value; }
    public String getMar() { return mar; }
    public void setMar(String value) { this.mar = value; }
    public LocalDate getDech() { return dech; }
    public void setDech(LocalDate value) { this.dech = value; }
    public String getAgsa() { return agsa; }
    public void setAgsa(String value) { this.agsa = value; }
    public String getAgde() { return agde; }
    public void setAgde(String value) { this.agde = value; }
    public String getDevc() { return devc; }
    public void setDevc(String value) { this.devc = value; }
    public BigDecimal getMctv() { return mctv; }
    public void setMctv(BigDecimal value) { this.mctv = value; }
    public String getPieo() { return pieo; }
    public void setPieo(String value) { this.pieo = value; }
    public String getIden() { return iden; }
    public void setIden(String value) { this.iden = value; }
    public String getLang() { return lang; }
    public void setLang(String value) { this.lang = value; }
    public String getLibnls() { return libnls; }
    public void setLibnls(String value) { this.libnls = value; }
    public String getModu() { return modu; }
    public void setModu(String value) { this.modu = value; }
    public String getRefdos() { return refdos; }
    public void setRefdos(String value) { this.refdos = value; }
    public String getRefana() { return refana; }
    public void setRefana(String value) { this.refana = value; }
    public String getLabel() { return label; }
    public void setLabel(String value) { this.label = value; }
    public String getNat() { return nat; }
    public void setNat(String value) { this.nat = value; }
    public String getEta() { return eta; }
    public void setEta(String value) { this.eta = value; }
    public String getDestana() { return destana; }
    public void setDestana(String value) { this.destana = value; }
    public String getFusion() { return fusion; }
    public void setFusion(String value) { this.fusion = value; }
}
