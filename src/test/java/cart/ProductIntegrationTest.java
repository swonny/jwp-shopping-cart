package cart;

import cart.dao.ProductDao;
import cart.dao.entity.ProductEntity;
import cart.dto.request.RequestCreateProductDto;
import cart.dto.request.RequestUpdateProductDto;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayNameGeneration(ReplaceUnderscores.class)
@SuppressWarnings("NonAsciiCharacters")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductIntegrationTest {

    private static final String EMAIL = "a@a.com";
    private static final String PASSWORD = "password1";

    @LocalServerPort
    private int port;

    @Autowired
    private ProductDao productDao;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @AfterEach
    void afterEach() {
    }

    @Test
    void 메인화면에_네비게이션_바가_표시된다() {
        final Response response = given()
                .log().all().accept(MediaType.TEXT_HTML_VALUE)
                .when()
                .get("/")
                .then()
                .log().all()
                .extract().response();

        assertThat(response.getBody().asString()).contains("상품목록", "장바구니", "설정", "관리자");
    }

    @Test
    void 메인화면에서_관리자를_클릭하면_관리자_페이지가_반환된다() {
        final Response response = given()
                .log().all().accept(MediaType.TEXT_HTML_VALUE)
                .when()
                .get("/admin")
                .then()
                .log().all()
                .extract().response();

        assertThat(response.getBody().asString()).contains("ID", "이름", "가격", "이미지", "Actions", "상품 추가");
    }

    @Test
    void 상품을_등록하면_상품_목록_페이지와_관리자_페이지에_추가된다() {
        // given
        final Response createResponse = given()
                .auth().preemptive().basic(EMAIL, PASSWORD)
                .log().all().contentType(ContentType.JSON)
                .body(new RequestCreateProductDto("치킨", 10_000, "치킨 사진"))
                .when()
                .post("/products")
                .then()
                .log().all()
                .extract().response();

        // when
        final Response userResponse = given()
                .auth().preemptive().basic(EMAIL, PASSWORD)
                .log().all().accept(MediaType.TEXT_HTML_VALUE)
                .when()
                .get("/")
                .then()
                .log().all()
                .extract().response();

        final Response adminResponse = given()
                .auth().preemptive().basic(EMAIL, PASSWORD)
                .log().all().accept(MediaType.TEXT_HTML_VALUE)
                .when()
                .get("/")
                .then()
                .log().all()
                .extract().response();

        // then
        assertSoftly(softly -> {
            softly.assertThat(createResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
            softly.assertThat(userResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
            softly.assertThat(userResponse.body().asString()).contains("치킨", "10000", "치킨 사진");
            softly.assertThat(adminResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
            softly.assertThat(adminResponse.body().asString()).contains("치킨", "10000", "치킨 사진");
        });
    }

    @Test
    void 상품을_삭제하면_상품_목록_페이지와_관리자_페이지에서_사라진다() {
        // given
        final Long insertedId = productDao.insert(
                new ProductEntity.Builder()
                        .name("치킨")
                        .price(10_000)
                        .image("치킨 사진")
                        .build()
        );

        final Response deleteResponse = given()
                .auth().preemptive().basic(EMAIL, PASSWORD)
                .log().all().accept(MediaType.TEXT_HTML_VALUE)
                .when()
                .delete("/products/" + insertedId)
                .then()
                .log().all()
                .extract().response();

        // when
        final Response userResponse = given()
                .auth().preemptive().basic(EMAIL, PASSWORD)
                .log().all().accept(MediaType.TEXT_HTML_VALUE)
                .when()
                .get("/")
                .then()
                .log().all()
                .extract().response();

        final Response adminResponse = given()
                .auth().preemptive().basic(EMAIL, PASSWORD)
                .log().all().accept(MediaType.TEXT_HTML_VALUE)
                .when()
                .get("/")
                .then()
                .log().all()
                .extract().response();

        // then
        assertSoftly(softly -> {
            softly.assertThat(deleteResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
            softly.assertThat(userResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
            softly.assertThat(userResponse.body().asString()).doesNotContain("치킨", "10000", "치킨 사진");
            softly.assertThat(adminResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
            softly.assertThat(adminResponse.body().asString()).doesNotContain("치킨", "10000", "치킨 사진");
        });
    }

    @Test
    void 등록한_상품을_수정하면_상품_목록_페이지와_관리자_페이지에서_수정된다() {
        // given
        final Long insertedId = productDao.insert(
                new ProductEntity.Builder()
                        .name("치킨")
                        .price(10_000)
                        .image("치킨 사진")
                        .build()
        );

        final Response updateResponse = given()
                .auth().preemptive().basic(EMAIL, PASSWORD)
                .log().all().contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(new RequestUpdateProductDto("피자", 1_000, "피자 사진"))
                .when()
                .put("/products/" + insertedId)
                .then()
                .log().all()
                .extract().response();

        // when
        final Response userResponse = given()
                .auth().preemptive().basic(EMAIL, PASSWORD)
                .log().all().accept(MediaType.TEXT_HTML_VALUE)
                .when()
                .get("/")
                .then()
                .log().all()
                .extract().response();

        final Response adminResponse = given()
                .auth().preemptive().basic(EMAIL, PASSWORD)
                .log().all().accept(MediaType.TEXT_HTML_VALUE)
                .when()
                .get("/")
                .then()
                .log().all()
                .extract().response();

        // then
        assertSoftly(softly -> {
            softly.assertThat(updateResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
            softly.assertThat(userResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
            softly.assertThat(userResponse.body().asString()).contains("피자", "1000", "피자 사진");
            softly.assertThat(adminResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
            softly.assertThat(adminResponse.body().asString()).contains("피자", "1000", "피자 사진");
        });
    }
}
