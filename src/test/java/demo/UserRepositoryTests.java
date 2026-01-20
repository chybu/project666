package demo;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import demo.user.User;
import demo.user.UserRepository;

@DataJpaTest
public class UserRepositoryTests {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    public void testCreateUser(){        
        User user = new User();
        user.setEmail("user1.yahoo.com");
        user.setUsername("user1");
        user.setPassword("password");

        User saved_user = userRepository.save(user);

        User exist_user = testEntityManager.find(User.class, saved_user.getId());

        Assertions.assertThat(exist_user.getEmail()).isEqualTo(saved_user.getEmail());
    }

    @Test
    public void testFindUserByEmail(){

        String test_email = "user1.yahoo.com";

        User user = new User();
        user.setEmail(test_email);
        user.setUsername("user1");
        user.setPassword("password");

        userRepository.save(user);

        User found_user = userRepository.findByEmail(test_email);

        Assertions.assertThat(found_user).isNotNull();

        String wrong_email = "user2.google.com";
        found_user = userRepository.findByEmail(wrong_email);

        Assertions.assertThat(found_user).isNull();
    }
}
