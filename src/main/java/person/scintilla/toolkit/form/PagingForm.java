package person.scintilla.toolkit.form;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.dbflute.cbean.result.PagingResultBean;
import org.dbflute.dbmeta.AbstractEntity;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;
import person.scintilla.toolkit.internal.ToolkitConfigManager;
import person.scintilla.toolkit.utils.DecimalUtils;

/**
 * @version 0.1.3 2025-09-28
 */
public class PagingForm extends BaseForm {

	private Integer pageNumber = 1;

	private Integer pageSize;

	private Integer displayPageCount;

	private Integer allRecordCount = 0;

	private Integer allPageCount = 0;

	private List<Integer> displayPageList;

	{
		Environment environment = getApplicationBean(Environment.class);
		this.setPageSize(DecimalUtils.toInteger(DecimalUtils.ifNullThen(
				environment.getProperty(ToolkitConfigManager.getConfig().getPageSizePropName()), 10)));
		this.setDisplayPageCount(DecimalUtils.toInteger(DecimalUtils.ifNullThen(
				environment.getProperty(ToolkitConfigManager.getConfig().getDisplayPageCountPropName()), 5)));
	}

	public void clearPagingInfo() {
		this.setPageNumber(null);
		this.setAllPageCount(0);
		this.setAllRecordCount(0);
	}

	public void pushPagingInfoFixPosition(PagingResultBean<? extends AbstractEntity> pagedResultList) {
		this.setAllRecordCount(pagedResultList.getAllRecordCount());
		this.setAllPageCount(pagedResultList.getAllPageCount());
		if (this.getAllRecordCount() == 0) {
			return;
		}
		BigDecimal pageNumber = new BigDecimal(this.getPageNumber());
		BigDecimal halfSize = new BigDecimal(this.getDisplayPageCount()).subtract(BigDecimal.ONE).divide(new BigDecimal(2));
		int beginPage = Math.max(pageNumber.subtract(halfSize).setScale(0, RoundingMode.CEILING).intValue(), 1);
		int endPage = Math.min(pageNumber.add(halfSize).setScale(0, RoundingMode.CEILING).intValue(), this.getAllPageCount());
		List<Integer> pageList = IntStream.rangeClosed(beginPage, endPage).boxed().collect(Collectors.toList());
		this.setDisplayPageList(pageList);
		}

	public void pushPagingInfoFixCount(PagingResultBean<? extends AbstractEntity> pagedResultList) {
		pushPagingInfoFixPosition(pagedResultList);
		if (!CollectionUtils.isEmpty(this.getDisplayPageList()) && !isFulfilled()) {
			if (haveFirstPage() && haveLastPage()) {
				return;
			}
			int targetPage;
			while (!isFulfilled() && haveFirstPage() && (targetPage = this.getLastDisplayedPage() + 1) <= this.getAllPageCount()) {
				this.getDisplayPageList().add(targetPage);
			}
			while (!isFulfilled() && haveLastPage() && (targetPage = this.getDisplayPageList().get(0) - 1) >= 1) {
				this.getDisplayPageList().add(0, targetPage);
			}
		}
	}

	private int getLastDisplayedPage() {
		return this.getDisplayPageList().get(this.getDisplayPageList().size() - 1);
	}

	private boolean isFulfilled() {
		return this.getDisplayPageList().size() == this.getDisplayPageCount();
	}

	private boolean haveFirstPage() {
		return this.getDisplayPageList().get(0) == 1;
	}

	private boolean haveLastPage() {
		return this.getLastDisplayedPage() == this.getAllPageCount();
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Integer getPageNumber() {
		return pageNumber;
	}
	public void setPageNumber(Integer pageNumber) {
		this.pageNumber = pageNumber;
	}
	public Integer getPageSize() {
		return pageSize;
	}
	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}
	public Integer getDisplayPageCount() {
		return displayPageCount;
	}
	public void setDisplayPageCount(Integer displayPageCount) {
		this.displayPageCount = displayPageCount;
	}
	public Integer getAllRecordCount() {
		return allRecordCount;
	}
	public void setAllRecordCount(Integer allRecordCount) {
		this.allRecordCount = allRecordCount;
	}
	public Integer getAllPageCount() {
		return allPageCount;
	}
	public void setAllPageCount(Integer allPageCount) {
		this.allPageCount = allPageCount;
	}

	public List<Integer> getDisplayPageList() {
		return displayPageList;
	}
	public void setDisplayPageList(List<Integer> displayPageList) {
		this.displayPageList = displayPageList;
	}

}
