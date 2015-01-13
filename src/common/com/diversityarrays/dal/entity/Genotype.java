/*
 * dalserver-interop library - implementation of DAL server for interoperability
 * Copyright (C) 2015  Diversity Arrays Technology
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.diversityarrays.dal.entity;
// Code was generated by SourceGen1 on 2014-06-19 at 12:03:29

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Table;
/**
 * <b>Region: Genotypes and Specimens</b>
 * <p>
 * List of genotypes available for trial units. 
 * Direct relation to the trial unit maybe sometimes
 * problematic, especially in horticulture when one plant can
 * be a single trial unit and can be a 'hybrid' of a few
 * genotypes.
 * <p>
 * This is why specimen is established as a subunit of the
 * genotype. In this case one genotype can have more than one
 * specimen (plants, plant groups), which may grow in various
 * locations.
 * <p>
 * Synonym for genotype can be variety and should be used as a
 * generic category.
 */

@Table(name="genotype")
@EntityTag("Genotype")
public class Genotype extends DalEntity {

	// TODO add column descriptions from the schema
	@Id
	@Column(name="GenotypeId", nullable=false)
	private Integer genotypeId;

	@Column(name="GenotypeName", nullable=false, length=(255))
	private String genotypeName;

	@JoinTable(name="genus",
            joinColumns=@JoinColumn(name="GenusId", referencedColumnName="GenusId")
        )
	@Column(name="GenusId", nullable=false)
	private Integer genusId;

	@Column(name="SpeciesName", nullable=true, length=(255))
	private String speciesName;

	@Column(name="GenotypeAcronym", nullable=true, length=(32))
	private String genotypeAcronym;

	@Column(name="OriginId", nullable=false)
	private Integer originId;

	@Column(name="CanPublishGenotype", nullable=false)
	private Boolean canPublishGenotype;

	@Column(name="GenotypeColor", nullable=true, length=(32))
	private String genotypeColor;

	@Column(name="GenotypeNote", nullable=true, length=(6000))
	private String genotypeNote;

	@Column(name="OwnGroupId", nullable=false)
	private Integer ownGroupId;

	@Column(name="AccessGroupId", nullable=false)
	private Integer accessGroupId;

	@Column(name="OwnGroupPerm", nullable=false)
	private Integer ownGroupPerm;

	@Column(name="AccessGroupPerm", nullable=false)
	private Integer accessGroupPerm;

	@Column(name="OtherPerm", nullable=false)
	private Integer otherPerm;
	
	// Extra
	@Column(name="GenusName", table="genus") // actually acquired via join
	// TODO handle this automatically in SQL construction
	private String genusName;

	public Genotype() {
		super();
	}
	
	@Override
	public String toString() {
		return genotypeName;
	}

	public Integer getGenotypeId() {
		return genotypeId;
	}

	public void setGenotypeId(Integer v) {
		this.genotypeId = v;
	}

	public String getGenotypeName() {
		return genotypeName;
	}

	public void setGenotypeName(String v) {
		this.genotypeName = v;
	}

	public Integer getGenusId() {
		return genusId;
	}

	public void setGenusId(Integer v) {
		this.genusId = v;
	}

	public String getSpeciesName() {
		return speciesName;
	}

	public void setSpeciesName(String v) {
		this.speciesName = v;
	}

	public String getGenotypeAcronym() {
		return genotypeAcronym;
	}

	public void setGenotypeAcronym(String v) {
		this.genotypeAcronym = v;
	}

	public Integer getOriginId() {
		return originId;
	}

	public void setOriginId(Integer v) {
		this.originId = v;
	}

	public Boolean getCanPublishGenotype() {
		return canPublishGenotype;
	}

	public void setCanPublishGenotype(Boolean v) {
		this.canPublishGenotype = v;
	}

	public String getGenotypeColor() {
		return genotypeColor;
	}

	public void setGenotypeColor(String v) {
		this.genotypeColor = v;
	}

	public String getGenotypeNote() {
		return genotypeNote;
	}

	public void setGenotypeNote(String v) {
		this.genotypeNote = v;
	}

	public Integer getOwnGroupId() {
		return ownGroupId;
	}

	public void setOwnGroupId(Integer v) {
		this.ownGroupId = v;
	}

	public Integer getAccessGroupId() {
		return accessGroupId;
	}

	public void setAccessGroupId(Integer v) {
		this.accessGroupId = v;
	}

	public Integer getOwnGroupPerm() {
		return ownGroupPerm;
	}

	public void setOwnGroupPerm(Integer v) {
		this.ownGroupPerm = v;
	}

	public Integer getAccessGroupPerm() {
		return accessGroupPerm;
	}

	public void setAccessGroupPerm(Integer v) {
		this.accessGroupPerm = v;
	}

	public Integer getOtherPerm() {
		return otherPerm;
	}

	public void setOtherPerm(Integer v) {
		this.otherPerm = v;
	}
	
	// Extras via join
	public String getGenusName() {
		return genusName;
	}
	
	public void setGenusName(String v) {
		this.genusName = v;
	}
	
	// Create the EntityColumn-s so we can do the ColumnNameMapping
	
	static public final EntityColumn GENOTYPE_ID = createEntityColumn(Genotype.class, "genotypeId");

	static public final EntityColumn GENOTYPE_NAME = createEntityColumn(Genotype.class, "genotypeName");

	static public final EntityColumn GENUS_ID = createEntityColumn(Genotype.class, "genusId");

	static public final EntityColumn SPECIES_NAME = createEntityColumn(Genotype.class, "speciesName");

	static public final EntityColumn GENOTYPE_ACRONYM = createEntityColumn(Genotype.class, "genotypeAcronym");

	static public final EntityColumn ORIGIN_ID = createEntityColumn(Genotype.class, "originId");

	static public final EntityColumn CAN_PUBLISH_GENOTYPE = createEntityColumn(Genotype.class, "canPublishGenotype");

	static public final EntityColumn GENOTYPE_COLOR = createEntityColumn(Genotype.class, "genotypeColor");

	static public final EntityColumn GENOTYPE_NOTE = createEntityColumn(Genotype.class, "genotypeNote");

	static public final EntityColumn GENUS_NAME = createEntityColumn(Genotype.class, "genusName");

	static public EntityColumn[] entityColumns() {
		return getEntityColumns(Genotype.class);
	}
}
