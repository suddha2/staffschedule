package staffschedule;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.midco.rota.RotaServiceApplication;
import com.midco.rota.converter.ShiftTypeListConverter;
import com.midco.rota.service.PayCycleDataService;
import com.midco.rota.util.PayCycleRow;

import jakarta.transaction.Transactional;


@SpringBootTest(classes=RotaServiceApplication.class)
@TestPropertySource(locations = "classpath:application-dev.properties")
@Transactional
class PayCycleDataServiceIntegrationTest {
    @Autowired
    private PayCycleDataService payCycleDataService;

    @Test
    void fetchRows_shouldReturnEnrichedRowsFromRealData() {
        String location = "Barnet";

        List<PayCycleRow> rows = payCycleDataService.fetchRows(location);

        assertFalse(rows.isEmpty(), "Expected at least one pay cycle row");

        for (PayCycleRow row : rows) {
            System.out.println("Period: " + row.getStartDate() + " → " + row.getEndDate());
            System.out.println("Has Solve Request: " + row.isHasSolveRequest());
            System.out.println("Employees: " + row.getEmployeeCount());
            System.out.println("Shifts: " + row.getShiftCount());
            System.out.println("Services: " + row.getLocationCount());
            System.out.println("ShiftStats: " + row.getShiftStats());
            System.out.println("ShiftAssignmentStats: " + row.getShiftAssignmentStats());
        } 
    }
    
    
    @Test
    void shiftTypeConverter() {
    	String testString="LONG_DAY,FLOAT";
    	
    	ShiftTypeListConverter converter= new ShiftTypeListConverter();
    	converter.convertToEntityAttribute(testString).forEach(System.out::println);;
    	
    }
}

