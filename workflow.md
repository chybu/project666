# Patient Homepage – Upcoming Appointments Workflow

I will describe the workflow in detail of how to get the upcoming appointments for the homepage of patient dashboard. For other information, you can follow this flow.

---

## Example workflow:

### 1.
So let say after the patient login, I want them to be redirected to "localhost:8080/homepage". First of all, i need to config controller class, specifically the:

frontend\src\main\java\com\project666\frontend\controller\WebController.java

In here there will be a method callback. This is the first endpoint the patient will go to after login via Keycloak. So after log in via Keycloak, Keycloak will redirect patient to:

localhost:8080/callback

No matter what role the user have, they will all be routed here. That's why I use WebController instead of specfic role controller like PatientController.

In this /callback endpoint, I want to create a method extract the user role and route the user to their correct homepage. So to decide what the server will do when they go to localhost:8080/callback, i need to create a method inside the WebController.

```java
public String callback(){

}
```

---

### 2.
I need to let server know to use this callback method when user go to "localhost:8080/callback", so i add the annotation. Without this, the server don't know what to do

```java
@GetMapping("/callback")
public String callback(){

}
```

---

### 3.
Then I need to extract the role of the user. This is stored inside the Authentication object. Spring will auto inject this object for me when I put them as the param of my method.

Notice to use the correct kind of Authentication:

```java
import org.springframework.security.core.Authentication;
```

Not just this one, there are many object using the same name but they from different package. Be careful.

```java
@GetMapping("/callback")
public String callback(
    Authentication authentication
){

}
```

---

### 4.
Then I will write a logic to extract user role and then based on the role to return to their corresponding homepage.

```java
@GetMapping("/callback")
public String callback(
    Authentication authentication
){
    if (authentication.getPrincipal() instanceof OidcUser oidcUser){
        userService.provisionUser(oidcUser);
    }

    RoleEnum role = getUserRole(authentication);
    if (RoleEnum.PATIENT.equals(role)){
        return "redirect:/patient/homepage";
    }
    else if(RoleEnum.DOCTOR.equals(role)){
        return "redirect:/doctor/homepage";
    }
    else if(RoleEnum.RECEPTIONIST.equals(role)){
        return "redirect:/receptionist/homepage";
    }

    return "redirect:/error";
}
```

---

### 5.
So in this case, because the user is patient, then the return will be "redirect:/patient/homepage".

Instead of redirect, you can use forward. But I don't need to hide my url details so i use redirect.

When the method return this string, the server will return:

localhost:8080/redirect:/patient/homepage

The browser now will go to:

localhost:8080/patient/homepage

---

### 6.
Again, what should server do when user go to "localhost:8080/patient/homepage". We need to let them know by using the controller.

Right now, the user is already identified as a Patient. So i will use:

frontend\src\main\java\com\project666\frontend\controller\PatientController.java

---

### 7.
We need to create a method to continue the flow:

```java
@GetMapping("/homepage")
public String loadHomePage(

){

}
```

---

### 8.
I use @GetMapping("/homepage") instead of @GetMapping("patient/homepage") because at the very start of the PatientController class, I have:

```java
@RequestMapping("/patient")
```

This makes all the annotation under PatientController class auto have the starting path as "/patient". So instead of having the /patient, it will be there by default

---

### 9.
Because I only want Patient to enter this endpoint, then I put:

```java
@PreAuthorize("hasRole('PATIENT')")
@GetMapping("/homepage")
public String loadHomePage(

){

}
```

---

### 9 (continue).
Let say I want to have the upcoming appointments of the patient in the homepage. So I need to prepare this data in the current method.

---

### 10.
So to get the appointments, I will use the backend service. Because this is a service related to appointments, I will check:

backend\src\main\java\com\project666\backend\service\AppointmentService.java

to see what method should i call.

Based on the documentation (I will document what each function to in the future. Forgive for my laziness), I will use:

```java
listDoctorAppointment(UUID patientId, ListAppointmentRequest request, Pageable pageable);
```

---

### 11.
First I need the patientId, This will be stored in OidcUser. Beside it, it also stores user info like lastname, firstname, etc. But this time I only get the id.

```java
@GetMapping("/homepage")
@PreAuthorize("hasRole('PATIENT')")
public String loadHomePage(
    @AuthenticationPrincipal OidcUser oidcUser
){
    UUID patientId = OidcUserUtil.getUserId(oidcUser);
}
```

---

### 12.
Second I need a Pageable. This will prevent the database return too much data. Instead, it will return page by page.

Let say the user have 100 appointments. You don't want to receive all 100 at the same times. Instead, there will be 5 pages, each page have 20 appointments. When user click a button, then call this service again but with different Pageable settings.

```java
@GetMapping("/homepage")
@PreAuthorize("hasRole('PATIENT')")
public String loadHomePage(
    @AuthenticationPrincipal OidcUser oidcUser
){
    UUID patientId = OidcUserUtil.getUserId(oidcUser);

    Pageable pageable = PageRequest.of(0, 5, Sort.by("startTime").ascending());

    Page<Appointment> appointmentPage = appointmentService.listDoctorAppointment(patientId, request, pageable);

}
```

In this case, I only return 5 most nearest upcoming appointments

---

### 13.
Last, you need a ListAppointmentRequest object in:

backend\src\main\java\com\project666\backend\domain\ListAppointmentRequest.java

```java
public class ListAppointmentRequest {
    private UUID patientId;
    private UUID doctorId;
    private AppointmentTypeEnum type;
    private AppointmentStatusEnum status;
    private LocalDate from;
    private LocalDate end;
}
```

This class act as a filter for listing appointment. There are 6 filters you can set.

For getting upcoming appointments of a patient, you will need:

- patientId as null. No matter what you put here, it won't be used because the service method don't use patientId from here but use the patientId you passed to it, which get from the oidc object previously. This prevent other patient can see appointment of other by changing their url  
- doctorId as null because we want to get all appointments no matter who is the doctor  
- type as null same logic as doctorId  
- status should be AppointmentStatusEnum.CONFIRMED because this is the ready appointment. Other type is complete and cancel, which is not for upcoming  
- from as now()  
- end as null same logic as doctorId  

```java
ListAppointmentRequest request = new ListAppointmentRequest();
request.setStatus(AppointmentStatusEnum.CONFIRMED);
request.setFrom(LocalDate.now());
```

---

### 14.
Now we have all params we need, I will call the AppointmentService to run the listDoctorAppointment method.

First I need a AppointmentService object. This is already injected at the beginning of the class:

```java
private final AppointmentService appointmentService;
```

Notice, there are no class constructor for PatientController but we still have the AppointmentService object to use.

Like for normal, we need:

```java
public PatientController(AppointmentService appointmentService){...}
```

But thanks to the `@RequiredArgsConstructor`, spring will auto deploy this for us.

---

### 15.
With the appointmentService, i will call the method and get the info I need:

```java
Page<Appointment> appointmentPage =
    appointmentService.listDoctorAppointment(patientId, request, pageable);
```

---

### 16.
Now we got the data, but how to pass it to the html template? We need a Model class.

```java
@GetMapping("/homepage")
@PreAuthorize("hasRole('PATIENT')")
public String loadHomePage(
    @AuthenticationPrincipal OidcUser oidcUser,
    Model model
){
    UUID patientId = OidcUserUtil.getUserId(oidcUser);

    Pageable pageable = PageRequest.of(0, 5, Sort.by("startTime").ascending());

    ListAppointmentRequest request = new ListAppointmentRequest();
    request.setStatus(AppointmentStatusEnum.CONFIRMED);
    request.setFrom(LocalDate.now());

    Page<Appointment> appointmentPage =
        appointmentService.listDoctorAppointment(patientId, request, pageable);

    model.addAttribute("appointments", appointmentPage.getContent());
}
```

---

### 17.
Now we need to decide what html to pass the data to, or what html to return when user go to "localhost:8080/patient/homepage".

```java
return "patient/homepage";
```

---

### 18.
To return the file at:

src/main/resources/templates/patient/homepage.html

I will return "patient/homepage".

By default, Spring checks the templates folder for HTML files and automatically adds the .html extension.

However, to return a file from the static folder, like:

src/main/resources/static/landingPage/index.html

I should return:

```java
return "forward:/landingPage/index.html";
```

We need a forward because when you return a simple string like "/landingPage/index.html", Spring thinks you are looking for a template.

Since that file lives in the static folder, not templates, Spring won't find it and will likely give you a 404 error or a circular view path error.

Don't use redirect because it will remove the data inside the Model.

---

### 19.
Now we have the data delivered to the html, how to load it from model and put it inside the page? We use Thymeleaf.

```html
<div class="right">
    <div class="upcoming">
        <span class="upcoming-title">Upcoming Important Dates</span>

        <div th:if="${#lists.isEmpty(appointments)}" class="no-data">
            No upcoming confirmed appointments.
        </div>

        <div th:each="appt : ${appointments}" class="appointment-item">
            
            <div class="appt-date" 
                th:text="${#temporals.format(appt.startTime, 'dd MMM yyyy HH:mm')}">
            </div>

            <div class="appt-type" th:text="${appt.type}"></div>

            <div class="appt-doctor" 
                th:text="'Dr. ' + ${appt.doctor.firstName}">
            </div>
        
        </div>
    </div>
</div>
```