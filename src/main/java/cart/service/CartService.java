package cart.service;

import cart.dao.CartDao;
import cart.dao.MemberDao;
import cart.dao.ProductDao;
import cart.dao.entity.CartEntity;
import cart.dao.entity.ProductEntity;
import cart.dto.auth.AuthInfo;
import cart.dto.request.RequestCreateProductDto;
import cart.dto.request.RequestUpdateProductDto;
import cart.dto.response.ResponseProductDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class CartService {

    private static final int MINIMUM_AFFECTED_ROWS = 1;

    private final ProductDao productDao;
    private final MemberDao memberDao;
    private final CartDao cartDao;

    @Autowired
    public CartService(final ProductDao productDao, final MemberDao memberDao, final CartDao cartDao) {
        this.productDao = productDao;
        this.memberDao = memberDao;
        this.cartDao = cartDao;
    }

    @Transactional(readOnly = true)
    public List<ResponseProductDto> findAll() {
        final List<ProductEntity> productEntities = productDao.selectAll();
        return productEntities.stream()
                .map(entity -> new ResponseProductDto(
                        entity.getId(),
                        entity.getName(),
                        entity.getPrice(),
                        entity.getImage())
                ).collect(Collectors.toUnmodifiableList());
    }

    @Transactional
    public void insert(final RequestCreateProductDto requestCreateProductDto) {
        final ProductEntity newProductEntity = new ProductEntity.Builder()
                .name(requestCreateProductDto.getName())
                .price(requestCreateProductDto.getPrice())
                .image(requestCreateProductDto.getImage())
                .build();
        productDao.insert(newProductEntity);
    }

    @Transactional
    public void update(final Long id, final RequestUpdateProductDto requestUpdateProductDto) {
        ProductEntity oldProductEntity = productDao.findById(id)
                .orElseThrow(() -> new NoSuchElementException("찾는 상품이 없습니다."));
        final ProductEntity productEntity = new ProductEntity.Builder()
                .id(id)
                .name(requestUpdateProductDto.getName().orElse(oldProductEntity.getName()))
                .price(requestUpdateProductDto.getPrice().orElse(oldProductEntity.getPrice()))
                .image(requestUpdateProductDto.getImage().orElse(oldProductEntity.getImage()))
                .build();
        final int updatedRows = productDao.update(productEntity);
        validateAffectedRowsCount(updatedRows);
    }

    private void validateAffectedRowsCount(final int affectedRows) {
        if (affectedRows < MINIMUM_AFFECTED_ROWS) {
            throw new IllegalArgumentException("접근하려는 데이터가 존재하지 않습니다.");
        }
    }

    @Transactional
    public void delete(final Long id) {
        final int affectedRows = productDao.delete(id);
        validateAffectedRowsCount(affectedRows);
    }

    public List<ResponseProductDto> findCartProductsByMember(final AuthInfo authInfo) {
        final Long memberId = memberDao.findIdByAuthInfo(authInfo.getEmail(), authInfo.getPassword());
        final List<ProductEntity> productEntities = cartDao.findProductsByMemberId(memberId);
        return productEntities.stream()
                .map(ResponseProductDto::transferEntityToDto)
                .collect(Collectors.toList());
    }

    public void addProductToCart(final Long memberId, final Long productId) {
        if (cartDao.hasSameProduct(memberId, productId)) {
            throw new IllegalArgumentException("카트에 이미 존재하는 상품입니다.");
        }
        final CartEntity cartEntity = new CartEntity.Builder()
                .memberId(memberId)
                .productId(productId)
                .build();
        cartDao.add(cartEntity);
    }

    public void deleteProductFromCart(final Long memberId, final Long productId) {
        final CartEntity cartEntity = new CartEntity.Builder()
                .memberId(memberId)
                .productId(productId)
                .build();
        cartDao.deleteProduct(cartEntity);
    }
}
