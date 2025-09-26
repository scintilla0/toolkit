package person.scintilla.toolkit.internal;

public class DefaultToolkitConfig implements ToolkitConfig {

	@Override
	public String getDefaultDateFormat() {
		return "yyyy/MM/dd";
	}

	@Override
	public String getPageSizePropName() {
		return "springboot.pj.paging.page-size";
	}

	@Override
	public String getDisplayPageCountPropName() {
		return "springboot.pj.paging.display-page-count";
	}

	@Override
	public String getListEmptyErrorCode() {
		return "{E000A001}";
	}

	@Override
	public String getRepeatErrorCode() {
		return "EC000A008";
	}

	@Override
	public String getEmptyErrorCode() {
		return "E000A001";
	}

	@Override
	public String getLengthErrorCode() {
		return "E000A002";
	}

	@Override
	public String getPatternNumberErrorCode() {
		return "E000A003";
	}

	@Override
	public String getPatternAlphaNumErrorCode() {
		return "E000A005";
	}

	@Override
	public String getPatternAlphaNumPuncErrorCode() {
		return "E000A023";
	}

}
