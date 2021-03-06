/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.fhir2.api.translators.impl;

import static org.apache.commons.lang.Validate.notNull;

import javax.inject.Inject;

import java.util.Collections;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.Setter;
import org.hl7.fhir.r4.model.Encounter;
import org.openmrs.EncounterProvider;
import org.openmrs.api.PatientService;
import org.openmrs.module.fhir2.api.translators.EncounterLocationTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterParticipantTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterTranslator;
import org.springframework.stereotype.Component;

@Component
@Setter(AccessLevel.PACKAGE)
public class EncounterTranslatorImpl extends AbstractReferenceHandlingTranslator implements EncounterTranslator {
	
	@Inject
	PatientService patientService;
	
	@Inject
	EncounterParticipantTranslator participantTranslator;
	
	@Inject
	EncounterLocationTranslator encounterLocationTranslator;
	
	@Override
	public Encounter toFhirResource(org.openmrs.Encounter openMrsEncounter) {
		Encounter encounter = new Encounter();
		encounter.setId(openMrsEncounter.getUuid());
		encounter.setStatus(Encounter.EncounterStatus.UNKNOWN);
		
		encounter.setSubject(createPatientReference(openMrsEncounter.getPatient()));
		encounter.setParticipant(openMrsEncounter.getEncounterProviders().stream().map(participantTranslator::toFhirResource)
		        .collect(Collectors.toList()));
		encounter.setLocation(
		    Collections.singletonList(encounterLocationTranslator.toFhirResource(openMrsEncounter.getLocation())));
		encounter.getMeta().setLastUpdated(openMrsEncounter.getDateChanged());
		
		return encounter;
	}
	
	@Override
	public org.openmrs.Encounter toOpenmrsType(Encounter fhirEncounter) {
		return this.toOpenmrsType(new org.openmrs.Encounter(), fhirEncounter);
	}
	
	@Override
	public org.openmrs.Encounter toOpenmrsType(org.openmrs.Encounter existingEncounter, Encounter encounter) {
		notNull(existingEncounter, "Existing encounter cannot be null");
		
		if (encounter == null) {
			return existingEncounter;
		}
		existingEncounter.setUuid(encounter.getId());
		String patientUuid = getReferenceId(encounter.getSubject());
		existingEncounter.setPatient(patientService.getPatientByUuid(patientUuid));
		existingEncounter
		        .setEncounterProviders(encounter
		                .getParticipant().stream().map(encounterParticipantComponent -> participantTranslator
		                        .toOpenmrsType(new EncounterProvider(), encounterParticipantComponent))
		                .collect(Collectors.toSet()));
		existingEncounter.setLocation(encounterLocationTranslator.toOpenmrsType(encounter.getLocationFirstRep()));
		existingEncounter.setDateChanged(encounter.getMeta().getLastUpdated());
		
		return existingEncounter;
	}
}
