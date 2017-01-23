package interview.treatment.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Your task is to implement the below service to solve the following problem:
 * given a Patient, what is the appropriate TreatmentPlan?
 *
 * A Patient has a name, date of birth, weight, list of symptoms, list of medication allergies,
 * and MRN (Medical Record Number). We have also provided a list of Diseases, Medications, and Clinics
 * for use in this problem in our test suite.
 *
 * A Disease has a name, list of symptoms (which suggest a patient has the disease if a patient has the
 * symptoms in the list), and a list of possible treatments for the disease. Each possible treatment for
 * a disease is a combination of medications with dosage amounts given in mg/kg.
 *
 * A Medication has a name and a cost per mg.
 *
 * A Clinic has a name, a range of ages (in months) that the clinic is open to, and a list of diseases
 * the clinic specializes in treating.
 *
 * Using this information and the provided classes and interface, implement the TreatmentPlanServiceImpl
 * class. Each method in the interface includes exact specifications for what it should return. You can validate
 * that you are returning the correct information using the provided JUnit Test Suite. We will test your answers
 * against additional tests upon your submission of your code.
 *
 * The "Init" method will be called before each test to set up the lists of Disease, Medications, and Clinics. We
 * may test your solution against different lists of Diseases, Medications, and Clinics.
 */
public class TreatmentPlanServiceImpl implements TreatmentPlanService {

    // Do not modify the lists below.
    private List<Disease> diseases = new ArrayList<>();
    private List<Medication> medications = new ArrayList<>();
    private List<Clinic> clinics = new ArrayList<>();

    // TODO Optionally Implement any additional data structures here....
    // Date against which age is measured for the purpose of this class
    private static final LocalDate SEPTEMBER_1_2016 = LocalDate.of(2016, Month.SEPTEMBER, 1);

    // TODO .... to here.

    @Override
    public void init(List<Disease> diseases, List<Clinic> clinics, List<Medication> medications) {

        this.diseases = diseases;
        this.clinics = clinics;
        this.medications = medications;

        // TODO Optionally implement any additional init items below here ....

        // TODO ... to here.
    }

    @Override
    public Integer ageInYears(Patient patient) {
        LocalDate dateOfBirth = patient.getDateOfBirth();

        return Period.between(dateOfBirth, SEPTEMBER_1_2016).getYears();
    }

    @Override
    public Integer ageInMonths(Patient patient) {
        LocalDate dateOfBirth = patient.getDateOfBirth();

        Period period = Period.between(dateOfBirth, SEPTEMBER_1_2016);

        return (period.getYears() * 12) + period.getMonths();
    }

    @Override
    public List<Clinic> clinicsBasedOnAgeAndDiseases(Patient patient) {
        int patientAgeInMonths = ageInMonths(patient);
        Map<Disease, BigDecimal> diseaseLikelihoods = diseaseLikelihoods(patient);

        // List of all clinics for which the patient's age is within the clinic's age range
        List<Clinic> validClinicsByAge = clinics
                .stream()
                .filter(clinic -> {
                    if(clinic.getMaxAgeInMonths() == null) {
                        return clinic.getMinAgeInMonths() <= patientAgeInMonths;
                    } else {
                        return clinic.getMinAgeInMonths() <= patientAgeInMonths &&
                                patientAgeInMonths <= clinic.getMaxAgeInMonths();
                    }
                })
                .collect(Collectors.toList());

        // List of clinics with appropriate age ranges that treat disease which the patient has >= 70% chance of having
        boolean doesClinicTreatDisease;
        boolean isPatientLikelyToHaveDisease;
        List<Clinic> validClinicsByDisease = new ArrayList<>();
        for(Clinic clinic : validClinicsByAge) {
            for(Map.Entry<Disease, BigDecimal> diseaseLikelihood: diseaseLikelihoods.entrySet()) {
                if(validClinicsByDisease.contains(clinic)) {
                    continue; // avoid adding duplicates
                }

                doesClinicTreatDisease = clinic.getDiseases().contains(diseaseLikelihood.getKey().getName());
                isPatientLikelyToHaveDisease = diseaseLikelihood.getValue().compareTo(BigDecimal.valueOf(.70)) != -1;

                if(doesClinicTreatDisease && isPatientLikelyToHaveDisease) {
                    validClinicsByDisease.add(clinic);
                }
            }
        }

        return validClinicsByDisease;
    }

    @Override
    public Map<Disease, BigDecimal> diseaseLikelihoods(Patient patient) {
        List<String> symptoms = patient.getSymptoms();
        Map<Disease, BigDecimal> diseaseLikelihoods = new HashMap<>();
        BigDecimal likelihood;
        List<String> commonSymptoms;
        // lengths stored as doubles to avoid extra casts when converting to BigDecimal
        double commonSymptomsLength;
        double diseaseSymptomsLength;

        for(Disease disease : diseases) {

            // Intersection of the disease's symptoms and the patient's symptoms
            commonSymptoms = disease
                    .getSymptoms()
                    .stream()
                    .filter(symptoms::contains)
                    .collect(Collectors.toList());

            commonSymptomsLength = commonSymptoms.size();
            diseaseSymptomsLength = disease.getSymptoms().size();
            likelihood = BigDecimal
                    .valueOf(commonSymptomsLength / diseaseSymptomsLength)
                    .setScale(2, BigDecimal.ROUND_HALF_DOWN);

            diseaseLikelihoods.put(disease, likelihood);
        }

        return diseaseLikelihoods;
    }

    @Override
    public Map<Medication, BigDecimal> medicationsForDisease(Patient patient, Disease disease) {
        Map<Medication, BigDecimal> medicationsForDisease = new HashMap<>();


        // Get all medication combinations that do not contain a medication the patient is allergic too
        List<Map<String, BigDecimal>> nonAllergicMedications = disease
                .getMedicationCombinations()
                .stream()
                .filter(medicationCombination ->
                        Collections.disjoint(patient.medicationAllergies(), medicationCombination.keySet()))
                .collect(Collectors.toList());

        // Compute the cost of each treatment and return the cheapest
        Map<String, BigDecimal> min = nonAllergicMedications.stream().min((o1, o2) -> {
            BigDecimal cost1 = BigDecimal
                    .valueOf(o1.values().stream().mapToDouble(BigDecimal::doubleValue).sum())
                    .multiply(patient.getWeight());
            BigDecimal cost2 = BigDecimal
                    .valueOf(o2.values().stream().mapToDouble(BigDecimal::doubleValue ).sum())
                    .multiply(patient.getWeight());

            return cost2.compareTo(cost2);
        }).get();

        Medication medication;
        for(Map.Entry<String, BigDecimal> entry : min.entrySet()) {
            // Find medication by name
            medication = medications
                    .stream()
                    .filter(m -> m.getName().equals(entry.getKey()))
                    .findFirst()
                    .get();

            // associate a medication with its dose considering patient weight
            medicationsForDisease.put(medication, entry.getValue().multiply(patient.getWeight()));
        }

        return medicationsForDisease;
    }

    @Override
    public TreatmentPlan treatmentPlanForPatient(Patient patient) {
        TreatmentPlan treatmentPlan = new TreatmentPlan();
        Map<Medication, BigDecimal> medicationsForTreatmentPlan = new HashMap<>();
        int ageMonthPortion = Period.between(patient.getDateOfBirth(), SEPTEMBER_1_2016).getMonths();

        treatmentPlan.setAgeYearPortion(ageInYears(patient));
        treatmentPlan.setAgeMonthPortion(ageMonthPortion);
        treatmentPlan.setClinics(clinicsBasedOnAgeAndDiseases(patient));


        // Check all disease likelihoods, and provide medication for all diseases
        // for which there exists at least a 70% that the patient has the disease
        Map<Disease, BigDecimal> diseaseLikelihoods = diseaseLikelihoods(patient);
        Map<Medication, BigDecimal> medicationCombinations;
        for(Map.Entry<Disease, BigDecimal> diseaseLikelihood : diseaseLikelihoods.entrySet()) {
            if(diseaseLikelihood.getValue().compareTo(BigDecimal.valueOf(.70)) != -1) {
                medicationCombinations = medicationsForDisease(patient, diseaseLikelihood.getKey());

                for(Map.Entry<Medication, BigDecimal> medicationCombination : medicationCombinations.entrySet()) {

                    // If the medication is already in the treatment plan and we try to add it again,
                    // we add the next dose to the current one
                    if(medicationsForTreatmentPlan.containsKey(medicationCombination.getKey())) {
                        medicationsForTreatmentPlan.put(
                                medicationCombination.getKey(),
                                medicationsForTreatmentPlan
                                        .get(medicationCombination.getKey())
                                        .add(medicationCombination.getValue())
                        );
                    } else {
                        medicationsForTreatmentPlan.put(
                                medicationCombination.getKey(),
                                medicationCombination.getValue()
                        );
                    }
                }
            }
        }

        treatmentPlan.setMedications(medicationsForTreatmentPlan);

        return treatmentPlan;
    }

    private Medication findMedicationByName(String name) {
        return medications
                .stream()
                .filter(medication -> medication.getName().equals(name))
                .findFirst()
                .get();
    }

    private Map<String, BigDecimal> findCheapestMedicationOption(List<Map<String, BigDecimal>> medicationOptions) {
        Map<String, BigDecimal> cheapestOption = new HashMap<>();

        if(medicationOptions.isEmpty()) {
            return new HashMap<>(); // return and empty map
        }

        if(medicationOptions.size() == 1) {
            return medicationOptions.get(0); // return the first, is it is lowest cost by default
        }

        // Otherwise, we much look for the min
        BigDecimal currentCost;
        BigDecimal lowestCost = getCostForMedicationOption(medicationOptions.get(0));
        cheapestOption = medicationOptions.get(0); // assume first is cheapest
        for(Map<String, BigDecimal> option : medicationOptions) {
            currentCost = getCostForMedicationOption(option);

            if(currentCost.compareTo(lowestCost) == -1) {
                lowestCost = getCostForMedicationOption(option);
                cheapestOption = option;
            }
        }

        return cheapestOption;
    }

    private BigDecimal getCostForMedicationOption(Map<String, BigDecimal> medicationOption) {
        Medication medication;
        BigDecimal cost = BigDecimal.ZERO;

        for(Map.Entry<String, BigDecimal> entry : medicationOption.entrySet()) {
            medication = findMedicationByName(entry.getKey());
            cost = cost.add(medication.getCostPerMg());
        }

        return cost;
    }
}
