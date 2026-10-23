package com.web.service;
import com.web.entity.Color;
import com.web.entity.Size;
import com.web.exception.MessageException;
import com.web.repository.SizeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ColorSizeValidationTests {
    @Test void rejectsUnsafeColorValue(){var c=new Color();c.setName("Đỏ");c.setCode("red; background:url(x)");assertThrows(MessageException.class,()->new ColorService().saveOrUpdate(c));}
    @Test void rejectsBlankSize(){var s=new Size();s.setName(" ");assertThrows(MessageException.class,()->new SizeService().saveOrUpdate(s));}
    @Test void sortsShoeNumbersBeforeLegacyLabels(){
        var repo=mock(SizeRepository.class);var service=new SizeService();ReflectionTestUtils.setField(service,"sizeRepository",repo);
        var a=new Size();a.setName("S");var b=new Size();b.setName("40");var c=new Size();c.setName("38");
        when(repo.findAll()).thenReturn(List.of(a,b,c));assertEquals(List.of("38","40","S"),service.findAllList().stream().map(Size::getName).toList());
    }
    @Test void normalizesNewSizeWithoutRenamingLegacyRows(){
        var repo=mock(SizeRepository.class);var service=new SizeService();ReflectionTestUtils.setField(service,"sizeRepository",repo);
        var s=new Size();s.setName(" 40.5 ");assertEquals("40.5",service.saveOrUpdate(s).getName());verify(repo).save(s);
    }
}
