package com.sixpay.payment.infrastructure.banking.amplitude.posting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Complete Regional PaymentProviderEvent wire schema, optional fields omitted. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AmplitudePaymentProviderEvent {
    private String paymentReference;
    private String operationCode;
    private String currency;
    private String nature;
    private String technicalUser;
    private String debtorAccountReference;
    private String creditorAccountReference;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private String label;
    private String agsa;
    private String age;
    private String csp1;
    private String ope;
    private String eve;
    private String typ;
    private String ndos;
    private String age1;
    private String dev1;
    private String ncp1;
    private String suf1;
    private String clc1;
    private String cli1;
    private String nom1;
    private String ges1;
    private String sen1;
    private BigDecimal mht1;
    private BigDecimal mon1;
    private LocalDate dva1;
    private String exo1;
    private BigDecimal sol1;
    private BigDecimal indh1;
    private BigDecimal inds1;
    private String desa1;
    private String desa2;
    private String desa3;
    private String desa4;
    private String desa5;
    private String age2;
    private String dev2;
    private String ncp2;
    private String suf2;
    private String clc2;
    private String cli2;
    private String nom2;
    private String ges2;
    private String sen2;
    private BigDecimal mht2;
    private BigDecimal mon2;
    private LocalDate dva2;
    private LocalDate din;
    private String exo2;
    private BigDecimal sol2;
    private BigDecimal indh2;
    private BigDecimal inds2;
    private String desc1;
    private String desc2;
    private String desc3;
    private String desc4;
    private String desc5;
    private String etab;
    private String guib;
    private String nome;
    private String domi;
    private String adb1;
    private String adb2;
    private String adb3;
    private String vilb;
    private String cpob;
    private String cpay;
    private String etabr;
    private String guibr;
    private String comb;
    private String cleb;
    private String nomb;
    private BigDecimal mban;
    private LocalDate dvab;
    private String cai1;
    private String tyc1;
    private String dcai1;
    private String scai1;
    private BigDecimal mcai1;
    private BigDecimal arrc1;
    private String cai2;
    private String tyc2;
    private String dcai2;
    private String scai2;
    private BigDecimal mcai2;
    private BigDecimal arrc2;
    private String dev;
    private BigDecimal mht;
    private BigDecimal mnat;
    private BigDecimal mbor;
    private String nbor;
    private Integer nblig;
    private BigDecimal tcai2;
    private BigDecimal tcai3;
    private String nat;
    private String nato;
    private String opeo;
    private String eveo;
    private String pieo;
    private LocalDate dou;
    private LocalDate dco;
    private String eta;
    private String etap;
    private Integer nbv;
    private Integer nval;
    private String uti;
    private String utf;
    private String uta;
    private BigDecimal moa;
    private BigDecimal mof;
    private String lib1;
    private String lib2;
    private String lib3;
    private String lib4;
    private String lib5;
    private String lib6;
    private String lib7;
    private String lib8;
    private String lib9;
    private String lib10;
    private String agec;
    private String agep;
    private String intr;
    private String orig;
    private String rlet;
    private String catr;
    private String ceb;
    private String plb;
    private String cco;
    private LocalDate dret;
    private String natp;
    private String nump;
    private String datp;
    private String nomp;
    private String ad1p;
    private String ad2p;
    private String delp;
    private String serie;
    private String nche;
    private String chql;
    private String chqc;
    private String cab;
    private String ncff;
    private String csa;
    private LocalDate dech;
    private String tire;
    private String agti;
    private String agre;
    private Integer nbji;
    private String ptfc;
    private String efav;
    private String navl;
    private String edom;
    private String eopp;
    private String efac;
    private String moti;
    private String envacc;
    private String enom;
    private String vicl;
    private String teco;
    private String tenv;
    private String exjo;
    private String org;
    private String cai3;
    private BigDecimal mcai3;
    private String forc;
    private String ocai3;
    private Integer ncai3;
    private String csp2;
    private String csp3;
    private String csp4;
    private String csp5;
    private String ndom;
    private String cmod;
    private String devf;
    private String ncpf;
    private String suff;
    private BigDecimal monf;
    private LocalDate dvaf;
    private String exof;
    private String agee;
    private String deve;
    private String ncpe;
    private String sufe;
    private String clce;
    private String ncpi;
    private String sufi;
    private BigDecimal mimp;
    private LocalDate dvai;
    private String ncpp;
    private String sufp;
    private BigDecimal prga;
    private BigDecimal mrga;
    private String term;
    private String tvar;
    private String intp;
    private String cap;
    private String prll;
    private String ano;
    private String etab1;
    private String guib1;
    private String com1b;
    private String etab2;
    private String guib2;
    private String com2b;
    private BigDecimal tcom1;
    private BigDecimal mcom1;
    private BigDecimal tcom2;
    private BigDecimal mcom2;
    private BigDecimal tcom3;
    private BigDecimal mcom3;
    private BigDecimal frai1;
    private BigDecimal frai2;
    private BigDecimal frai3;
    private BigDecimal ttax1;
    private BigDecimal mtax1;
    private BigDecimal ttax2;
    private BigDecimal mtax2;
    private BigDecimal ttax3;
    private BigDecimal mtax3;
    private BigDecimal mnt1;
    private BigDecimal mnt2;
    private BigDecimal mnt3;
    private BigDecimal mnt4;
    private BigDecimal mnt5;
    private String tyc3;
    private String dcai3;
    private String scai3;
    private BigDecimal arrc3;
    private BigDecimal mhtd;
    private BigDecimal tcai4;
    private String tope;
    private String img;
    private LocalDate dsai;
    private String hsai;
    private String paysp;
    private String pdelp;
    private String manda;
    private String refdos;
    private BigDecimal tchfr;

    public AmplitudePaymentProviderEvent() {
    }

    public AmplitudePaymentProviderEvent(
            String paymentReference, String operationCode, String currency,
            String nature, String technicalUser, String debtorAccountReference,
            String creditorAccountReference, BigDecimal debitAmount,
            BigDecimal creditAmount, String label) {
        this.paymentReference = paymentReference;
        this.operationCode = operationCode;
        this.currency = currency;
        this.nature = nature;
        this.technicalUser = technicalUser;
        this.debtorAccountReference = debtorAccountReference;
        this.creditorAccountReference = creditorAccountReference;
        this.debitAmount = debitAmount;
        this.creditAmount = creditAmount;
        this.label = label;
    }

    public String paymentReference() { return paymentReference; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String value) { this.paymentReference = value; }

    public String operationCode() { return operationCode; }
    public String getOperationCode() { return operationCode; }
    public void setOperationCode(String value) { this.operationCode = value; }

    public String currency() { return currency; }
    public String getCurrency() { return currency; }
    public void setCurrency(String value) { this.currency = value; }

    public String nature() { return nature; }
    public String getNature() { return nature; }
    public void setNature(String value) { this.nature = value; }

    public String technicalUser() { return technicalUser; }
    public String getTechnicalUser() { return technicalUser; }
    public void setTechnicalUser(String value) { this.technicalUser = value; }

    public String debtorAccountReference() { return debtorAccountReference; }
    public String getDebtorAccountReference() { return debtorAccountReference; }
    public void setDebtorAccountReference(String value) { this.debtorAccountReference = value; }

    public String creditorAccountReference() { return creditorAccountReference; }
    public String getCreditorAccountReference() { return creditorAccountReference; }
    public void setCreditorAccountReference(String value) { this.creditorAccountReference = value; }

    public BigDecimal debitAmount() { return debitAmount; }
    public BigDecimal getDebitAmount() { return debitAmount; }
    public void setDebitAmount(BigDecimal value) { this.debitAmount = value; }

    public BigDecimal creditAmount() { return creditAmount; }
    public BigDecimal getCreditAmount() { return creditAmount; }
    public void setCreditAmount(BigDecimal value) { this.creditAmount = value; }

    public String label() { return label; }
    public String getLabel() { return label; }
    public void setLabel(String value) { this.label = value; }

    public String agsa() { return agsa; }
    public String getAgsa() { return agsa; }
    public void setAgsa(String value) { this.agsa = value; }

    public String age() { return age; }
    public String getAge() { return age; }
    public void setAge(String value) { this.age = value; }

    public String csp1() { return csp1; }
    public String getCsp1() { return csp1; }
    public void setCsp1(String value) { this.csp1 = value; }

    public String ope() { return ope; }
    public String getOpe() { return ope; }
    public void setOpe(String value) { this.ope = value; }

    public String eve() { return eve; }
    public String getEve() { return eve; }
    public void setEve(String value) { this.eve = value; }

    public String typ() { return typ; }
    public String getTyp() { return typ; }
    public void setTyp(String value) { this.typ = value; }

    public String ndos() { return ndos; }
    public String getNdos() { return ndos; }
    public void setNdos(String value) { this.ndos = value; }

    public String age1() { return age1; }
    public String getAge1() { return age1; }
    public void setAge1(String value) { this.age1 = value; }

    public String dev1() { return dev1; }
    public String getDev1() { return dev1; }
    public void setDev1(String value) { this.dev1 = value; }

    public String ncp1() { return ncp1; }
    public String getNcp1() { return ncp1; }
    public void setNcp1(String value) { this.ncp1 = value; }

    public String suf1() { return suf1; }
    public String getSuf1() { return suf1; }
    public void setSuf1(String value) { this.suf1 = value; }

    public String clc1() { return clc1; }
    public String getClc1() { return clc1; }
    public void setClc1(String value) { this.clc1 = value; }

    public String cli1() { return cli1; }
    public String getCli1() { return cli1; }
    public void setCli1(String value) { this.cli1 = value; }

    public String nom1() { return nom1; }
    public String getNom1() { return nom1; }
    public void setNom1(String value) { this.nom1 = value; }

    public String ges1() { return ges1; }
    public String getGes1() { return ges1; }
    public void setGes1(String value) { this.ges1 = value; }

    public String sen1() { return sen1; }
    public String getSen1() { return sen1; }
    public void setSen1(String value) { this.sen1 = value; }

    public BigDecimal mht1() { return mht1; }
    public BigDecimal getMht1() { return mht1; }
    public void setMht1(BigDecimal value) { this.mht1 = value; }

    public BigDecimal mon1() { return mon1; }
    public BigDecimal getMon1() { return mon1; }
    public void setMon1(BigDecimal value) { this.mon1 = value; }

    public LocalDate dva1() { return dva1; }
    public LocalDate getDva1() { return dva1; }
    public void setDva1(LocalDate value) { this.dva1 = value; }

    public String exo1() { return exo1; }
    public String getExo1() { return exo1; }
    public void setExo1(String value) { this.exo1 = value; }

    public BigDecimal sol1() { return sol1; }
    public BigDecimal getSol1() { return sol1; }
    public void setSol1(BigDecimal value) { this.sol1 = value; }

    public BigDecimal indh1() { return indh1; }
    public BigDecimal getIndh1() { return indh1; }
    public void setIndh1(BigDecimal value) { this.indh1 = value; }

    public BigDecimal inds1() { return inds1; }
    public BigDecimal getInds1() { return inds1; }
    public void setInds1(BigDecimal value) { this.inds1 = value; }

    public String desa1() { return desa1; }
    public String getDesa1() { return desa1; }
    public void setDesa1(String value) { this.desa1 = value; }

    public String desa2() { return desa2; }
    public String getDesa2() { return desa2; }
    public void setDesa2(String value) { this.desa2 = value; }

    public String desa3() { return desa3; }
    public String getDesa3() { return desa3; }
    public void setDesa3(String value) { this.desa3 = value; }

    public String desa4() { return desa4; }
    public String getDesa4() { return desa4; }
    public void setDesa4(String value) { this.desa4 = value; }

    public String desa5() { return desa5; }
    public String getDesa5() { return desa5; }
    public void setDesa5(String value) { this.desa5 = value; }

    public String age2() { return age2; }
    public String getAge2() { return age2; }
    public void setAge2(String value) { this.age2 = value; }

    public String dev2() { return dev2; }
    public String getDev2() { return dev2; }
    public void setDev2(String value) { this.dev2 = value; }

    public String ncp2() { return ncp2; }
    public String getNcp2() { return ncp2; }
    public void setNcp2(String value) { this.ncp2 = value; }

    public String suf2() { return suf2; }
    public String getSuf2() { return suf2; }
    public void setSuf2(String value) { this.suf2 = value; }

    public String clc2() { return clc2; }
    public String getClc2() { return clc2; }
    public void setClc2(String value) { this.clc2 = value; }

    public String cli2() { return cli2; }
    public String getCli2() { return cli2; }
    public void setCli2(String value) { this.cli2 = value; }

    public String nom2() { return nom2; }
    public String getNom2() { return nom2; }
    public void setNom2(String value) { this.nom2 = value; }

    public String ges2() { return ges2; }
    public String getGes2() { return ges2; }
    public void setGes2(String value) { this.ges2 = value; }

    public String sen2() { return sen2; }
    public String getSen2() { return sen2; }
    public void setSen2(String value) { this.sen2 = value; }

    public BigDecimal mht2() { return mht2; }
    public BigDecimal getMht2() { return mht2; }
    public void setMht2(BigDecimal value) { this.mht2 = value; }

    public BigDecimal mon2() { return mon2; }
    public BigDecimal getMon2() { return mon2; }
    public void setMon2(BigDecimal value) { this.mon2 = value; }

    public LocalDate dva2() { return dva2; }
    public LocalDate getDva2() { return dva2; }
    public void setDva2(LocalDate value) { this.dva2 = value; }

    public LocalDate din() { return din; }
    public LocalDate getDin() { return din; }
    public void setDin(LocalDate value) { this.din = value; }

    public String exo2() { return exo2; }
    public String getExo2() { return exo2; }
    public void setExo2(String value) { this.exo2 = value; }

    public BigDecimal sol2() { return sol2; }
    public BigDecimal getSol2() { return sol2; }
    public void setSol2(BigDecimal value) { this.sol2 = value; }

    public BigDecimal indh2() { return indh2; }
    public BigDecimal getIndh2() { return indh2; }
    public void setIndh2(BigDecimal value) { this.indh2 = value; }

    public BigDecimal inds2() { return inds2; }
    public BigDecimal getInds2() { return inds2; }
    public void setInds2(BigDecimal value) { this.inds2 = value; }

    public String desc1() { return desc1; }
    public String getDesc1() { return desc1; }
    public void setDesc1(String value) { this.desc1 = value; }

    public String desc2() { return desc2; }
    public String getDesc2() { return desc2; }
    public void setDesc2(String value) { this.desc2 = value; }

    public String desc3() { return desc3; }
    public String getDesc3() { return desc3; }
    public void setDesc3(String value) { this.desc3 = value; }

    public String desc4() { return desc4; }
    public String getDesc4() { return desc4; }
    public void setDesc4(String value) { this.desc4 = value; }

    public String desc5() { return desc5; }
    public String getDesc5() { return desc5; }
    public void setDesc5(String value) { this.desc5 = value; }

    public String etab() { return etab; }
    public String getEtab() { return etab; }
    public void setEtab(String value) { this.etab = value; }

    public String guib() { return guib; }
    public String getGuib() { return guib; }
    public void setGuib(String value) { this.guib = value; }

    public String nome() { return nome; }
    public String getNome() { return nome; }
    public void setNome(String value) { this.nome = value; }

    public String domi() { return domi; }
    public String getDomi() { return domi; }
    public void setDomi(String value) { this.domi = value; }

    public String adb1() { return adb1; }
    public String getAdb1() { return adb1; }
    public void setAdb1(String value) { this.adb1 = value; }

    public String adb2() { return adb2; }
    public String getAdb2() { return adb2; }
    public void setAdb2(String value) { this.adb2 = value; }

    public String adb3() { return adb3; }
    public String getAdb3() { return adb3; }
    public void setAdb3(String value) { this.adb3 = value; }

    public String vilb() { return vilb; }
    public String getVilb() { return vilb; }
    public void setVilb(String value) { this.vilb = value; }

    public String cpob() { return cpob; }
    public String getCpob() { return cpob; }
    public void setCpob(String value) { this.cpob = value; }

    public String cpay() { return cpay; }
    public String getCpay() { return cpay; }
    public void setCpay(String value) { this.cpay = value; }

    public String etabr() { return etabr; }
    public String getEtabr() { return etabr; }
    public void setEtabr(String value) { this.etabr = value; }

    public String guibr() { return guibr; }
    public String getGuibr() { return guibr; }
    public void setGuibr(String value) { this.guibr = value; }

    public String comb() { return comb; }
    public String getComb() { return comb; }
    public void setComb(String value) { this.comb = value; }

    public String cleb() { return cleb; }
    public String getCleb() { return cleb; }
    public void setCleb(String value) { this.cleb = value; }

    public String nomb() { return nomb; }
    public String getNomb() { return nomb; }
    public void setNomb(String value) { this.nomb = value; }

    public BigDecimal mban() { return mban; }
    public BigDecimal getMban() { return mban; }
    public void setMban(BigDecimal value) { this.mban = value; }

    public LocalDate dvab() { return dvab; }
    public LocalDate getDvab() { return dvab; }
    public void setDvab(LocalDate value) { this.dvab = value; }

    public String cai1() { return cai1; }
    public String getCai1() { return cai1; }
    public void setCai1(String value) { this.cai1 = value; }

    public String tyc1() { return tyc1; }
    public String getTyc1() { return tyc1; }
    public void setTyc1(String value) { this.tyc1 = value; }

    public String dcai1() { return dcai1; }
    public String getDcai1() { return dcai1; }
    public void setDcai1(String value) { this.dcai1 = value; }

    public String scai1() { return scai1; }
    public String getScai1() { return scai1; }
    public void setScai1(String value) { this.scai1 = value; }

    public BigDecimal mcai1() { return mcai1; }
    public BigDecimal getMcai1() { return mcai1; }
    public void setMcai1(BigDecimal value) { this.mcai1 = value; }

    public BigDecimal arrc1() { return arrc1; }
    public BigDecimal getArrc1() { return arrc1; }
    public void setArrc1(BigDecimal value) { this.arrc1 = value; }

    public String cai2() { return cai2; }
    public String getCai2() { return cai2; }
    public void setCai2(String value) { this.cai2 = value; }

    public String tyc2() { return tyc2; }
    public String getTyc2() { return tyc2; }
    public void setTyc2(String value) { this.tyc2 = value; }

    public String dcai2() { return dcai2; }
    public String getDcai2() { return dcai2; }
    public void setDcai2(String value) { this.dcai2 = value; }

    public String scai2() { return scai2; }
    public String getScai2() { return scai2; }
    public void setScai2(String value) { this.scai2 = value; }

    public BigDecimal mcai2() { return mcai2; }
    public BigDecimal getMcai2() { return mcai2; }
    public void setMcai2(BigDecimal value) { this.mcai2 = value; }

    public BigDecimal arrc2() { return arrc2; }
    public BigDecimal getArrc2() { return arrc2; }
    public void setArrc2(BigDecimal value) { this.arrc2 = value; }

    public String dev() { return dev; }
    public String getDev() { return dev; }
    public void setDev(String value) { this.dev = value; }

    public BigDecimal mht() { return mht; }
    public BigDecimal getMht() { return mht; }
    public void setMht(BigDecimal value) { this.mht = value; }

    public BigDecimal mnat() { return mnat; }
    public BigDecimal getMnat() { return mnat; }
    public void setMnat(BigDecimal value) { this.mnat = value; }

    public BigDecimal mbor() { return mbor; }
    public BigDecimal getMbor() { return mbor; }
    public void setMbor(BigDecimal value) { this.mbor = value; }

    public String nbor() { return nbor; }
    public String getNbor() { return nbor; }
    public void setNbor(String value) { this.nbor = value; }

    public Integer nblig() { return nblig; }
    public Integer getNblig() { return nblig; }
    public void setNblig(Integer value) { this.nblig = value; }

    public BigDecimal tcai2() { return tcai2; }
    public BigDecimal getTcai2() { return tcai2; }
    public void setTcai2(BigDecimal value) { this.tcai2 = value; }

    public BigDecimal tcai3() { return tcai3; }
    public BigDecimal getTcai3() { return tcai3; }
    public void setTcai3(BigDecimal value) { this.tcai3 = value; }

    public String nat() { return nat; }
    public String getNat() { return nat; }
    public void setNat(String value) { this.nat = value; }

    public String nato() { return nato; }
    public String getNato() { return nato; }
    public void setNato(String value) { this.nato = value; }

    public String opeo() { return opeo; }
    public String getOpeo() { return opeo; }
    public void setOpeo(String value) { this.opeo = value; }

    public String eveo() { return eveo; }
    public String getEveo() { return eveo; }
    public void setEveo(String value) { this.eveo = value; }

    public String pieo() { return pieo; }
    public String getPieo() { return pieo; }
    public void setPieo(String value) { this.pieo = value; }

    public LocalDate dou() { return dou; }
    public LocalDate getDou() { return dou; }
    public void setDou(LocalDate value) { this.dou = value; }

    public LocalDate dco() { return dco; }
    public LocalDate getDco() { return dco; }
    public void setDco(LocalDate value) { this.dco = value; }

    public String eta() { return eta; }
    public String getEta() { return eta; }
    public void setEta(String value) { this.eta = value; }

    public String etap() { return etap; }
    public String getEtap() { return etap; }
    public void setEtap(String value) { this.etap = value; }

    public Integer nbv() { return nbv; }
    public Integer getNbv() { return nbv; }
    public void setNbv(Integer value) { this.nbv = value; }

    public Integer nval() { return nval; }
    public Integer getNval() { return nval; }
    public void setNval(Integer value) { this.nval = value; }

    public String uti() { return uti; }
    public String getUti() { return uti; }
    public void setUti(String value) { this.uti = value; }

    public String utf() { return utf; }
    public String getUtf() { return utf; }
    public void setUtf(String value) { this.utf = value; }

    public String uta() { return uta; }
    public String getUta() { return uta; }
    public void setUta(String value) { this.uta = value; }

    public BigDecimal moa() { return moa; }
    public BigDecimal getMoa() { return moa; }
    public void setMoa(BigDecimal value) { this.moa = value; }

    public BigDecimal mof() { return mof; }
    public BigDecimal getMof() { return mof; }
    public void setMof(BigDecimal value) { this.mof = value; }

    public String lib1() { return lib1; }
    public String getLib1() { return lib1; }
    public void setLib1(String value) { this.lib1 = value; }

    public String lib2() { return lib2; }
    public String getLib2() { return lib2; }
    public void setLib2(String value) { this.lib2 = value; }

    public String lib3() { return lib3; }
    public String getLib3() { return lib3; }
    public void setLib3(String value) { this.lib3 = value; }

    public String lib4() { return lib4; }
    public String getLib4() { return lib4; }
    public void setLib4(String value) { this.lib4 = value; }

    public String lib5() { return lib5; }
    public String getLib5() { return lib5; }
    public void setLib5(String value) { this.lib5 = value; }

    public String lib6() { return lib6; }
    public String getLib6() { return lib6; }
    public void setLib6(String value) { this.lib6 = value; }

    public String lib7() { return lib7; }
    public String getLib7() { return lib7; }
    public void setLib7(String value) { this.lib7 = value; }

    public String lib8() { return lib8; }
    public String getLib8() { return lib8; }
    public void setLib8(String value) { this.lib8 = value; }

    public String lib9() { return lib9; }
    public String getLib9() { return lib9; }
    public void setLib9(String value) { this.lib9 = value; }

    public String lib10() { return lib10; }
    public String getLib10() { return lib10; }
    public void setLib10(String value) { this.lib10 = value; }

    public String agec() { return agec; }
    public String getAgec() { return agec; }
    public void setAgec(String value) { this.agec = value; }

    public String agep() { return agep; }
    public String getAgep() { return agep; }
    public void setAgep(String value) { this.agep = value; }

    public String intr() { return intr; }
    public String getIntr() { return intr; }
    public void setIntr(String value) { this.intr = value; }

    public String orig() { return orig; }
    public String getOrig() { return orig; }
    public void setOrig(String value) { this.orig = value; }

    public String rlet() { return rlet; }
    public String getRlet() { return rlet; }
    public void setRlet(String value) { this.rlet = value; }

    public String catr() { return catr; }
    public String getCatr() { return catr; }
    public void setCatr(String value) { this.catr = value; }

    public String ceb() { return ceb; }
    public String getCeb() { return ceb; }
    public void setCeb(String value) { this.ceb = value; }

    public String plb() { return plb; }
    public String getPlb() { return plb; }
    public void setPlb(String value) { this.plb = value; }

    public String cco() { return cco; }
    public String getCco() { return cco; }
    public void setCco(String value) { this.cco = value; }

    public LocalDate dret() { return dret; }
    public LocalDate getDret() { return dret; }
    public void setDret(LocalDate value) { this.dret = value; }

    public String natp() { return natp; }
    public String getNatp() { return natp; }
    public void setNatp(String value) { this.natp = value; }

    public String nump() { return nump; }
    public String getNump() { return nump; }
    public void setNump(String value) { this.nump = value; }

    public String datp() { return datp; }
    public String getDatp() { return datp; }
    public void setDatp(String value) { this.datp = value; }

    public String nomp() { return nomp; }
    public String getNomp() { return nomp; }
    public void setNomp(String value) { this.nomp = value; }

    public String ad1p() { return ad1p; }
    public String getAd1p() { return ad1p; }
    public void setAd1p(String value) { this.ad1p = value; }

    public String ad2p() { return ad2p; }
    public String getAd2p() { return ad2p; }
    public void setAd2p(String value) { this.ad2p = value; }

    public String delp() { return delp; }
    public String getDelp() { return delp; }
    public void setDelp(String value) { this.delp = value; }

    public String serie() { return serie; }
    public String getSerie() { return serie; }
    public void setSerie(String value) { this.serie = value; }

    public String nche() { return nche; }
    public String getNche() { return nche; }
    public void setNche(String value) { this.nche = value; }

    public String chql() { return chql; }
    public String getChql() { return chql; }
    public void setChql(String value) { this.chql = value; }

    public String chqc() { return chqc; }
    public String getChqc() { return chqc; }
    public void setChqc(String value) { this.chqc = value; }

    public String cab() { return cab; }
    public String getCab() { return cab; }
    public void setCab(String value) { this.cab = value; }

    public String ncff() { return ncff; }
    public String getNcff() { return ncff; }
    public void setNcff(String value) { this.ncff = value; }

    public String csa() { return csa; }
    public String getCsa() { return csa; }
    public void setCsa(String value) { this.csa = value; }

    public LocalDate dech() { return dech; }
    public LocalDate getDech() { return dech; }
    public void setDech(LocalDate value) { this.dech = value; }

    public String tire() { return tire; }
    public String getTire() { return tire; }
    public void setTire(String value) { this.tire = value; }

    public String agti() { return agti; }
    public String getAgti() { return agti; }
    public void setAgti(String value) { this.agti = value; }

    public String agre() { return agre; }
    public String getAgre() { return agre; }
    public void setAgre(String value) { this.agre = value; }

    public Integer nbji() { return nbji; }
    public Integer getNbji() { return nbji; }
    public void setNbji(Integer value) { this.nbji = value; }

    public String ptfc() { return ptfc; }
    public String getPtfc() { return ptfc; }
    public void setPtfc(String value) { this.ptfc = value; }

    public String efav() { return efav; }
    public String getEfav() { return efav; }
    public void setEfav(String value) { this.efav = value; }

    public String navl() { return navl; }
    public String getNavl() { return navl; }
    public void setNavl(String value) { this.navl = value; }

    public String edom() { return edom; }
    public String getEdom() { return edom; }
    public void setEdom(String value) { this.edom = value; }

    public String eopp() { return eopp; }
    public String getEopp() { return eopp; }
    public void setEopp(String value) { this.eopp = value; }

    public String efac() { return efac; }
    public String getEfac() { return efac; }
    public void setEfac(String value) { this.efac = value; }

    public String moti() { return moti; }
    public String getMoti() { return moti; }
    public void setMoti(String value) { this.moti = value; }

    public String envacc() { return envacc; }
    public String getEnvacc() { return envacc; }
    public void setEnvacc(String value) { this.envacc = value; }

    public String enom() { return enom; }
    public String getEnom() { return enom; }
    public void setEnom(String value) { this.enom = value; }

    public String vicl() { return vicl; }
    public String getVicl() { return vicl; }
    public void setVicl(String value) { this.vicl = value; }

    public String teco() { return teco; }
    public String getTeco() { return teco; }
    public void setTeco(String value) { this.teco = value; }

    public String tenv() { return tenv; }
    public String getTenv() { return tenv; }
    public void setTenv(String value) { this.tenv = value; }

    public String exjo() { return exjo; }
    public String getExjo() { return exjo; }
    public void setExjo(String value) { this.exjo = value; }

    public String org() { return org; }
    public String getOrg() { return org; }
    public void setOrg(String value) { this.org = value; }

    public String cai3() { return cai3; }
    public String getCai3() { return cai3; }
    public void setCai3(String value) { this.cai3 = value; }

    public BigDecimal mcai3() { return mcai3; }
    public BigDecimal getMcai3() { return mcai3; }
    public void setMcai3(BigDecimal value) { this.mcai3 = value; }

    public String forc() { return forc; }
    public String getForc() { return forc; }
    public void setForc(String value) { this.forc = value; }

    public String ocai3() { return ocai3; }
    public String getOcai3() { return ocai3; }
    public void setOcai3(String value) { this.ocai3 = value; }

    public Integer ncai3() { return ncai3; }
    public Integer getNcai3() { return ncai3; }
    public void setNcai3(Integer value) { this.ncai3 = value; }

    public String csp2() { return csp2; }
    public String getCsp2() { return csp2; }
    public void setCsp2(String value) { this.csp2 = value; }

    public String csp3() { return csp3; }
    public String getCsp3() { return csp3; }
    public void setCsp3(String value) { this.csp3 = value; }

    public String csp4() { return csp4; }
    public String getCsp4() { return csp4; }
    public void setCsp4(String value) { this.csp4 = value; }

    public String csp5() { return csp5; }
    public String getCsp5() { return csp5; }
    public void setCsp5(String value) { this.csp5 = value; }

    public String ndom() { return ndom; }
    public String getNdom() { return ndom; }
    public void setNdom(String value) { this.ndom = value; }

    public String cmod() { return cmod; }
    public String getCmod() { return cmod; }
    public void setCmod(String value) { this.cmod = value; }

    public String devf() { return devf; }
    public String getDevf() { return devf; }
    public void setDevf(String value) { this.devf = value; }

    public String ncpf() { return ncpf; }
    public String getNcpf() { return ncpf; }
    public void setNcpf(String value) { this.ncpf = value; }

    public String suff() { return suff; }
    public String getSuff() { return suff; }
    public void setSuff(String value) { this.suff = value; }

    public BigDecimal monf() { return monf; }
    public BigDecimal getMonf() { return monf; }
    public void setMonf(BigDecimal value) { this.monf = value; }

    public LocalDate dvaf() { return dvaf; }
    public LocalDate getDvaf() { return dvaf; }
    public void setDvaf(LocalDate value) { this.dvaf = value; }

    public String exof() { return exof; }
    public String getExof() { return exof; }
    public void setExof(String value) { this.exof = value; }

    public String agee() { return agee; }
    public String getAgee() { return agee; }
    public void setAgee(String value) { this.agee = value; }

    public String deve() { return deve; }
    public String getDeve() { return deve; }
    public void setDeve(String value) { this.deve = value; }

    public String ncpe() { return ncpe; }
    public String getNcpe() { return ncpe; }
    public void setNcpe(String value) { this.ncpe = value; }

    public String sufe() { return sufe; }
    public String getSufe() { return sufe; }
    public void setSufe(String value) { this.sufe = value; }

    public String clce() { return clce; }
    public String getClce() { return clce; }
    public void setClce(String value) { this.clce = value; }

    public String ncpi() { return ncpi; }
    public String getNcpi() { return ncpi; }
    public void setNcpi(String value) { this.ncpi = value; }

    public String sufi() { return sufi; }
    public String getSufi() { return sufi; }
    public void setSufi(String value) { this.sufi = value; }

    public BigDecimal mimp() { return mimp; }
    public BigDecimal getMimp() { return mimp; }
    public void setMimp(BigDecimal value) { this.mimp = value; }

    public LocalDate dvai() { return dvai; }
    public LocalDate getDvai() { return dvai; }
    public void setDvai(LocalDate value) { this.dvai = value; }

    public String ncpp() { return ncpp; }
    public String getNcpp() { return ncpp; }
    public void setNcpp(String value) { this.ncpp = value; }

    public String sufp() { return sufp; }
    public String getSufp() { return sufp; }
    public void setSufp(String value) { this.sufp = value; }

    public BigDecimal prga() { return prga; }
    public BigDecimal getPrga() { return prga; }
    public void setPrga(BigDecimal value) { this.prga = value; }

    public BigDecimal mrga() { return mrga; }
    public BigDecimal getMrga() { return mrga; }
    public void setMrga(BigDecimal value) { this.mrga = value; }

    public String term() { return term; }
    public String getTerm() { return term; }
    public void setTerm(String value) { this.term = value; }

    public String tvar() { return tvar; }
    public String getTvar() { return tvar; }
    public void setTvar(String value) { this.tvar = value; }

    public String intp() { return intp; }
    public String getIntp() { return intp; }
    public void setIntp(String value) { this.intp = value; }

    public String cap() { return cap; }
    public String getCap() { return cap; }
    public void setCap(String value) { this.cap = value; }

    public String prll() { return prll; }
    public String getPrll() { return prll; }
    public void setPrll(String value) { this.prll = value; }

    public String ano() { return ano; }
    public String getAno() { return ano; }
    public void setAno(String value) { this.ano = value; }

    public String etab1() { return etab1; }
    public String getEtab1() { return etab1; }
    public void setEtab1(String value) { this.etab1 = value; }

    public String guib1() { return guib1; }
    public String getGuib1() { return guib1; }
    public void setGuib1(String value) { this.guib1 = value; }

    public String com1b() { return com1b; }
    public String getCom1b() { return com1b; }
    public void setCom1b(String value) { this.com1b = value; }

    public String etab2() { return etab2; }
    public String getEtab2() { return etab2; }
    public void setEtab2(String value) { this.etab2 = value; }

    public String guib2() { return guib2; }
    public String getGuib2() { return guib2; }
    public void setGuib2(String value) { this.guib2 = value; }

    public String com2b() { return com2b; }
    public String getCom2b() { return com2b; }
    public void setCom2b(String value) { this.com2b = value; }

    public BigDecimal tcom1() { return tcom1; }
    public BigDecimal getTcom1() { return tcom1; }
    public void setTcom1(BigDecimal value) { this.tcom1 = value; }

    public BigDecimal mcom1() { return mcom1; }
    public BigDecimal getMcom1() { return mcom1; }
    public void setMcom1(BigDecimal value) { this.mcom1 = value; }

    public BigDecimal tcom2() { return tcom2; }
    public BigDecimal getTcom2() { return tcom2; }
    public void setTcom2(BigDecimal value) { this.tcom2 = value; }

    public BigDecimal mcom2() { return mcom2; }
    public BigDecimal getMcom2() { return mcom2; }
    public void setMcom2(BigDecimal value) { this.mcom2 = value; }

    public BigDecimal tcom3() { return tcom3; }
    public BigDecimal getTcom3() { return tcom3; }
    public void setTcom3(BigDecimal value) { this.tcom3 = value; }

    public BigDecimal mcom3() { return mcom3; }
    public BigDecimal getMcom3() { return mcom3; }
    public void setMcom3(BigDecimal value) { this.mcom3 = value; }

    public BigDecimal frai1() { return frai1; }
    public BigDecimal getFrai1() { return frai1; }
    public void setFrai1(BigDecimal value) { this.frai1 = value; }

    public BigDecimal frai2() { return frai2; }
    public BigDecimal getFrai2() { return frai2; }
    public void setFrai2(BigDecimal value) { this.frai2 = value; }

    public BigDecimal frai3() { return frai3; }
    public BigDecimal getFrai3() { return frai3; }
    public void setFrai3(BigDecimal value) { this.frai3 = value; }

    public BigDecimal ttax1() { return ttax1; }
    public BigDecimal getTtax1() { return ttax1; }
    public void setTtax1(BigDecimal value) { this.ttax1 = value; }

    public BigDecimal mtax1() { return mtax1; }
    public BigDecimal getMtax1() { return mtax1; }
    public void setMtax1(BigDecimal value) { this.mtax1 = value; }

    public BigDecimal ttax2() { return ttax2; }
    public BigDecimal getTtax2() { return ttax2; }
    public void setTtax2(BigDecimal value) { this.ttax2 = value; }

    public BigDecimal mtax2() { return mtax2; }
    public BigDecimal getMtax2() { return mtax2; }
    public void setMtax2(BigDecimal value) { this.mtax2 = value; }

    public BigDecimal ttax3() { return ttax3; }
    public BigDecimal getTtax3() { return ttax3; }
    public void setTtax3(BigDecimal value) { this.ttax3 = value; }

    public BigDecimal mtax3() { return mtax3; }
    public BigDecimal getMtax3() { return mtax3; }
    public void setMtax3(BigDecimal value) { this.mtax3 = value; }

    public BigDecimal mnt1() { return mnt1; }
    public BigDecimal getMnt1() { return mnt1; }
    public void setMnt1(BigDecimal value) { this.mnt1 = value; }

    public BigDecimal mnt2() { return mnt2; }
    public BigDecimal getMnt2() { return mnt2; }
    public void setMnt2(BigDecimal value) { this.mnt2 = value; }

    public BigDecimal mnt3() { return mnt3; }
    public BigDecimal getMnt3() { return mnt3; }
    public void setMnt3(BigDecimal value) { this.mnt3 = value; }

    public BigDecimal mnt4() { return mnt4; }
    public BigDecimal getMnt4() { return mnt4; }
    public void setMnt4(BigDecimal value) { this.mnt4 = value; }

    public BigDecimal mnt5() { return mnt5; }
    public BigDecimal getMnt5() { return mnt5; }
    public void setMnt5(BigDecimal value) { this.mnt5 = value; }

    public String tyc3() { return tyc3; }
    public String getTyc3() { return tyc3; }
    public void setTyc3(String value) { this.tyc3 = value; }

    public String dcai3() { return dcai3; }
    public String getDcai3() { return dcai3; }
    public void setDcai3(String value) { this.dcai3 = value; }

    public String scai3() { return scai3; }
    public String getScai3() { return scai3; }
    public void setScai3(String value) { this.scai3 = value; }

    public BigDecimal arrc3() { return arrc3; }
    public BigDecimal getArrc3() { return arrc3; }
    public void setArrc3(BigDecimal value) { this.arrc3 = value; }

    public BigDecimal mhtd() { return mhtd; }
    public BigDecimal getMhtd() { return mhtd; }
    public void setMhtd(BigDecimal value) { this.mhtd = value; }

    public BigDecimal tcai4() { return tcai4; }
    public BigDecimal getTcai4() { return tcai4; }
    public void setTcai4(BigDecimal value) { this.tcai4 = value; }

    public String tope() { return tope; }
    public String getTope() { return tope; }
    public void setTope(String value) { this.tope = value; }

    public String img() { return img; }
    public String getImg() { return img; }
    public void setImg(String value) { this.img = value; }

    public LocalDate dsai() { return dsai; }
    public LocalDate getDsai() { return dsai; }
    public void setDsai(LocalDate value) { this.dsai = value; }

    public String hsai() { return hsai; }
    public String getHsai() { return hsai; }
    public void setHsai(String value) { this.hsai = value; }

    public String paysp() { return paysp; }
    public String getPaysp() { return paysp; }
    public void setPaysp(String value) { this.paysp = value; }

    public String pdelp() { return pdelp; }
    public String getPdelp() { return pdelp; }
    public void setPdelp(String value) { this.pdelp = value; }

    public String manda() { return manda; }
    public String getManda() { return manda; }
    public void setManda(String value) { this.manda = value; }

    public String refdos() { return refdos; }
    public String getRefdos() { return refdos; }
    public void setRefdos(String value) { this.refdos = value; }

    public BigDecimal tchfr() { return tchfr; }
    public BigDecimal getTchfr() { return tchfr; }
    public void setTchfr(BigDecimal value) { this.tchfr = value; }

}
